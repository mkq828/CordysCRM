<template>
  <div class="script-list-page flex h-full flex-col overflow-hidden">
    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <CrmSearchInput
          v-model:value="keyword"
          :placeholder="t('script.searchPlaceholder')"
          class="!w-[240px]"
          @search="search"
        />
        <n-select
          v-model:value="category"
          :options="categoryOptions"
          :placeholder="t('script.allCategory')"
          clearable
          class="!w-[180px]"
          @update:value="handleCategoryChange"
        />
        <div class="flex-1" />
        <n-button type="primary" @click="openAdd">{{ t('script.addScript') }}</n-button>
      </div>
    </CrmCard>

    <CrmCard no-content-padding hide-footer class="min-h-0 flex-1">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="script-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
        @refresh="propsEvent.refresh"
      />
    </CrmCard>

    <!-- 新增 / 编辑话术 -->
    <n-modal v-model:show="showForm" preset="card" :title="formTitle" class="!w-[560px]" :mask-closable="false">
      <n-form ref="formRef" :model="form" label-placement="left" :rules="rules" label-width="72">
        <n-form-item :label="t('script.category')" path="category">
          <n-select
            v-model:value="form.category"
            filterable
            tag
            clearable
            :options="categoryOptions"
            :placeholder="t('script.selectCategory')"
          />
        </n-form-item>
        <n-form-item :label="t('script.title')" path="title">
          <n-input v-model:value="form.title" :maxlength="128" :placeholder="t('script.titlePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('script.content')" path="content">
          <n-input
            v-model:value="form.content"
            type="textarea"
            :autosize="{ minRows: 4, maxRows: 10 }"
            :maxlength="5000"
            :placeholder="t('script.contentPlaceholder')"
          />
        </n-form-item>
        <n-form-item :label="t('script.source')" path="source">
          <n-input v-model:value="form.source" :maxlength="255" :placeholder="t('script.sourcePlaceholder')" />
        </n-form-item>
      </n-form>
      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showForm = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="saving" @click="confirmSave">{{ t('common.confirm') }}</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { h } from 'vue';
  import { NButton, NForm, NFormItem, NInput, NModal, NSelect, NTag, useMessage } from 'naive-ui';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { AiSalesScript, AiSalesScriptSaveParams } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import {
    addAiSalesScript,
    deleteAiSalesScript,
    getAiSalesScriptCategories,
    getAiSalesScriptPage,
    updateAiSalesScript,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const keyword = ref('');
  const category = ref<string | null>(null);

  const categoryOptions = ref<Array<{ label: string; value: string }>>([]);
  async function loadCategories() {
    try {
      const res = await getAiSalesScriptCategories();
      categoryOptions.value = (res || []).map((item) => ({ label: item, value: item }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const groupList: ActionsItem[] = [
    { label: t('common.edit'), key: 'edit' },
    { label: t('common.delete'), key: 'delete', danger: true },
  ];

  const tableRefreshId = ref(0);

  // ==================== 新增 / 编辑 ====================
  const showForm = ref(false);
  const saving = ref(false);
  const formRef = ref<InstanceType<typeof NForm>>();
  const form = reactive<AiSalesScriptSaveParams>({
    id: undefined,
    category: undefined,
    title: '',
    content: '',
    source: undefined,
  });

  const rules = {
    title: [{ required: true, message: t('common.notNull', { value: t('script.title') }), trigger: 'input' }],
    content: [{ required: true, message: t('common.notNull', { value: t('script.content') }), trigger: 'input' }],
  };

  const formTitle = computed(() => (form.id ? t('script.editScript') : t('script.addScript')));

  function resetForm() {
    form.id = undefined;
    form.category = undefined;
    form.title = '';
    form.content = '';
    form.source = undefined;
  }

  function openAdd() {
    resetForm();
    showForm.value = true;
  }

  function openEdit(row: AiSalesScript) {
    form.id = row.id;
    form.category = row.category;
    form.title = row.title || '';
    form.content = row.content || '';
    form.source = row.source;
    showForm.value = true;
  }

  function confirmSave() {
    formRef.value?.validate(async (errors) => {
      if (errors) return;
      saving.value = true;
      try {
        if (form.id) {
          await updateAiSalesScript({ ...form });
          Message.success(t('common.updateSuccess'));
        } else {
          await addAiSalesScript({ ...form });
          Message.success(t('common.addSuccess'));
        }
        showForm.value = false;
        tableRefreshId.value += 1;
        loadCategories();
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      } finally {
        saving.value = false;
      }
    });
  }

  // ==================== 删除 ====================
  function confirmDelete(row: AiSalesScript) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.title) }),
      content: t('script.deleteTip'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      positiveButtonProps: {
        type: 'error',
        size: 'medium',
      },
      onPositiveClick: async () => {
        try {
          await deleteAiSalesScript(row.id as string);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
          loadCategories();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleActionSelect(row: AiSalesScript, key: string) {
    switch (key) {
      case 'edit':
        openEdit(row);
        break;
      case 'delete':
        confirmDelete(row);
        break;
      default:
        break;
    }
  }

  const columns: CrmDataTableColumn<AiSalesScript>[] = [
    {
      title: t('script.title'),
      key: 'title',
      width: 200,
      fixed: 'left',
      ellipsis: { tooltip: true },
    },
    {
      title: t('script.category'),
      key: 'category',
      width: 130,
      render: (row: AiSalesScript) =>
        row.category ? h(NTag, { size: 'small', round: true, bordered: false }, { default: () => row.category }) : '-',
    },
    {
      title: t('script.content'),
      key: 'content',
      width: 320,
      ellipsis: { tooltip: true },
    },
    {
      title: t('script.source'),
      key: 'source',
      width: 150,
      ellipsis: { tooltip: true },
      render: (row: AiSalesScript) => row.source || '-',
    },
    {
      title: t('script.updateTime'),
      key: 'updateTime',
      width: 170,
    },
    {
      title: t('common.operation'),
      key: 'operation',
      width: 120,
      fixed: 'right',
      render: (row: AiSalesScript) =>
        h(CrmOperationButton, {
          groupList,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<AiSalesScript>(getAiSalesScriptPage, {
    tableKey: TableKeyEnum.AI_SALES_SCRIPT,
    columns,
    showSetting: true,
    containerClass: '.script-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  watch(tableRefreshId, () => {
    loadList();
  });

  function search(val?: string) {
    const params: Record<string, unknown> = {};
    if ((val ?? keyword.value).trim()) params.keyword = (val ?? keyword.value).trim();
    if (category.value) params.category = category.value;
    setLoadListParams(params);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function handleCategoryChange() {
    search();
  }

  onMounted(() => {
    loadCategories();
    search();
  });
</script>

<style lang="less" scoped></style>
