<template>
  <div class="doc-list-page flex h-full flex-col overflow-hidden">
    <div class="mb-[8px] text-[13px] text-orange-500">{{ t('knowledge.docDesc') }}</div>

    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <CrmSearchInput
          v-model:value="keyword"
          :placeholder="t('knowledge.searchPlaceholder')"
          class="!w-[240px]"
          @search="search"
        />
        <div class="flex-1" />
        <n-upload :show-file-list="false" accept=".pdf,.docx,.md,.txt" multiple :custom-request="handleUpload">
          <n-button type="primary">{{ t('knowledge.upload') }}</n-button>
        </n-upload>
      </div>
    </CrmCard>

    <CrmCard no-content-padding hide-footer class="min-h-0 flex-1">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="doc-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
        @refresh="propsEvent.refresh"
      />
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { h } from 'vue';
  import { NButton, NTag, NUpload, useMessage } from 'naive-ui';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit, formatFileSize } from '@lib/shared/method';
  import type { AiKnowledgeDoc } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import { deleteAiKnowledgeDoc, getAiKnowledgeDocPage, uploadKnowledgeDoc } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  import type { UploadCustomRequestOptions } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const keyword = ref('');

  const groupList: ActionsItem[] = [{ label: t('common.delete'), key: 'delete', danger: true }];

  const tableRefreshId = ref(0);

  // ==================== 上传 ====================
  function handleUpload(options: UploadCustomRequestOptions) {
    const { file } = options.file;
    if (!file) {
      options.onError();
      return;
    }
    uploadKnowledgeDoc(file)
      .then(() => {
        options.onFinish();
        Message.success(t('common.addSuccess'));
        tableRefreshId.value += 1;
      })
      .catch((error) => {
        options.onError();
        // eslint-disable-next-line no-console
        console.error(error);
      });
  }

  // ==================== 删除 ====================
  function confirmDelete(row: AiKnowledgeDoc) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('knowledge.deleteTip'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      positiveButtonProps: {
        type: 'error',
        size: 'medium',
      },
      onPositiveClick: async () => {
        try {
          await deleteAiKnowledgeDoc(row.id as string);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleActionSelect(row: AiKnowledgeDoc, key: string) {
    if (key === 'delete') {
      confirmDelete(row);
    }
  }

  const columns: CrmDataTableColumn<AiKnowledgeDoc>[] = [
    {
      title: t('knowledge.docName'),
      key: 'name',
      width: 240,
      fixed: 'left',
      ellipsis: { tooltip: true },
    },
    {
      title: t('knowledge.fileType'),
      key: 'fileType',
      width: 90,
      render: (row: AiKnowledgeDoc) =>
        row.fileType
          ? h(NTag, { size: 'small', round: true, bordered: false }, { default: () => row.fileType!.toUpperCase() })
          : '-',
    },
    {
      title: t('knowledge.fileSize'),
      key: 'fileSize',
      width: 110,
      render: (row: AiKnowledgeDoc) => (row.fileSize != null ? formatFileSize(row.fileSize) : '-'),
    },
    {
      title: t('knowledge.chunkCount'),
      key: 'chunkCount',
      width: 90,
      render: (row: AiKnowledgeDoc) => row.chunkCount ?? '-',
    },
    {
      title: t('knowledge.status'),
      key: 'status',
      width: 100,
      render: (row: AiKnowledgeDoc) =>
        row.status === 'FAILED'
          ? h(NTag, { size: 'small', type: 'error', bordered: false }, { default: () => t('knowledge.statusFailed') })
          : h(NTag, { size: 'small', type: 'success', bordered: false }, { default: () => t('knowledge.statusReady') }),
    },
    {
      title: t('knowledge.errorMsg'),
      key: 'errorMsg',
      width: 200,
      ellipsis: { tooltip: true },
      render: (row: AiKnowledgeDoc) => row.errorMsg || '-',
    },
    {
      title: t('knowledge.createTime'),
      key: 'createTime',
      width: 170,
    },
    {
      title: t('common.operation'),
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row: AiKnowledgeDoc) =>
        h(CrmOperationButton, {
          groupList,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<AiKnowledgeDoc>(getAiKnowledgeDocPage, {
    tableKey: TableKeyEnum.AI_KNOWLEDGE_DOC,
    columns,
    showSetting: true,
    containerClass: '.doc-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  watch(tableRefreshId, () => {
    loadList();
  });

  function search(val?: string) {
    const params: Record<string, unknown> = {};
    if ((val ?? keyword.value).trim()) params.keyword = (val ?? keyword.value).trim();
    setLoadListParams(params);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  onMounted(() => {
    search();
  });
</script>

<style lang="less" scoped></style>
