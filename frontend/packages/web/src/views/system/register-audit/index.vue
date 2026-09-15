<template>
  <div class="register-audit-page">
    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <n-input
          v-model:value="keyword"
          :placeholder="t('registerAudit.keywordPlaceholder')"
          clearable
          class="w-[280px]"
          @keydown.enter="search"
        />
        <n-select
          v-model:value="queryType"
          :options="typeOptions"
          :placeholder="t('registerAudit.type')"
          clearable
          class="w-[160px]"
        />
        <n-select
          v-model:value="queryStatus"
          :options="statusOptions"
          :placeholder="t('registerAudit.status')"
          clearable
          class="w-[160px]"
        />
        <n-button type="primary" @click="search">{{ t('common.search') }}</n-button>
        <n-button class="outline--secondary" @click="reset">{{ t('common.reset') }}</n-button>
      </div>
    </CrmCard>

    <CrmCard no-content-padding hide-footer :special-height="licenseStore.expiredDuring ? 272 : 0">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="crm-register-audit-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
      />
    </CrmCard>

    <!-- 详情弹窗 -->
    <n-modal
      v-model:show="showDetail"
      preset="card"
      :title="t('registerAudit.detailTitle')"
      class="w-[560px]"
      :mask-closable="false"
    >
      <n-spin :show="detailLoading">
        <n-descriptions v-if="detail" label-placement="left" :column="2" bordered size="small">
          <n-descriptions-item :label="t('registerAudit.name')" :span="2">{{ detail.name }}</n-descriptions-item>
          <n-descriptions-item :label="t('registerAudit.type')">{{ typeLabel(detail.type) }}</n-descriptions-item>
          <n-descriptions-item :label="t('registerAudit.phone')">{{ detail.phone }}</n-descriptions-item>
          <n-descriptions-item :label="t('registerAudit.idCard')">{{ detail.idCard || '-' }}</n-descriptions-item>
          <n-descriptions-item :label="t('registerAudit.verifyStatus')">{{
            statusLabel(detail.verifyStatus)
          }}</n-descriptions-item>
          <template v-if="detail.type === 'ENTERPRISE'">
            <n-descriptions-item :label="t('registerAudit.unifiedSocialCreditCode')" :span="2">
              {{ detail.unifiedSocialCreditCode || '-' }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('registerAudit.legalPersonName')" :span="2">
              {{ detail.legalPersonName || '-' }}
            </n-descriptions-item>
          </template>
          <n-descriptions-item
            v-if="detail.businessLicenseAttachmentId"
            :label="t('registerAudit.businessLicense')"
            :span="2"
          >
            <n-image :src="licensePreviewUrl(detail.businessLicenseAttachmentId)" width="320" object-fit="contain" />
          </n-descriptions-item>
          <n-descriptions-item v-if="detail.verifyRemark" :label="t('registerAudit.verifyRemark')" :span="2">
            {{ detail.verifyRemark }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('registerAudit.createTime')" :span="2">
            {{ formatTime(detail.createTime) }}
          </n-descriptions-item>
          <n-descriptions-item v-if="detail.verifyTime" :label="t('registerAudit.verifyTime')" :span="2">
            {{ formatTime(detail.verifyTime) }}
          </n-descriptions-item>
        </n-descriptions>
      </n-spin>
    </n-modal>

    <!-- 驳回弹窗 -->
    <n-modal
      v-model:show="showReject"
      preset="card"
      :title="t('registerAudit.rejectTitle')"
      class="w-[480px]"
      :mask-closable="false"
    >
      <n-input
        v-model:value="rejectRemark"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-count
        :placeholder="t('registerAudit.rejectPlaceholder')"
      />
      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showReject = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="rejectLoading" @click="confirmReject">
            {{ t('common.confirm') }}
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, ref, watch } from 'vue';
  import {
    NButton,
    NDescriptions,
    NDescriptionsItem,
    NImage,
    NInput,
    NModal,
    NSelect,
    NSpin,
    NTag,
    useMessage,
  } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { RegisterAuditItem, RegisterType, RegisterVerifyStatus } from '@lib/shared/models/system/register';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import { registerApprove, registerDetail, registerPageList, registerReject } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useLicenseStore from '@/store/modules/setting/license';
  import useUserStore from '@/store/modules/user';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const licenseStore = useLicenseStore();
  const userStore = useUserStore();

  const keyword = ref('');
  const queryType = ref<RegisterType | ''>('');
  const queryStatus = ref<RegisterVerifyStatus | ''>('');
  const tableRefreshId = ref(0);

  const typeOptions = computed(() => [
    { label: t('registerAudit.type.personal'), value: 'PERSONAL' },
    { label: t('registerAudit.type.enterprise'), value: 'ENTERPRISE' },
  ]);

  const statusOptions = computed(() => [
    { label: t('registerAudit.status.pending'), value: 'PENDING' },
    { label: t('registerAudit.status.approved'), value: 'APPROVED' },
    { label: t('registerAudit.status.rejected'), value: 'REJECTED' },
  ]);

  function typeLabel(type: RegisterType) {
    return type === 'ENTERPRISE' ? t('registerAudit.type.enterprise') : t('registerAudit.type.personal');
  }

  function statusLabel(status: RegisterVerifyStatus) {
    return t(`registerAudit.status.${status.toLowerCase()}`);
  }

  function statusTagType(status: RegisterVerifyStatus) {
    if (status === 'APPROVED') return 'success';
    if (status === 'REJECTED') return 'error';
    return 'warning';
  }

  function formatTime(ts?: number) {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm:ss') : '-';
  }

  function licensePreviewUrl(attachmentId: string) {
    return `${PreviewAttachmentUrl}/${attachmentId}?userId=${userStore.userInfo.id}`;
  }

  // 详情
  const showDetail = ref(false);
  const detailLoading = ref(false);
  const detail = ref<RegisterAuditItem | null>(null);

  async function openDetail(row: RegisterAuditItem) {
    showDetail.value = true;
    detailLoading.value = true;
    detail.value = null;
    try {
      detail.value = await registerDetail(row.id);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      detailLoading.value = false;
    }
  }

  // 通过
  function handleApprove(row: RegisterAuditItem) {
    openModal({
      type: 'default',
      title: t('registerAudit.approveTip'),
      content: t('registerAudit.approveTipContent'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await registerApprove({ id: row.id });
          Message.success(t('registerAudit.approveSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 驳回
  const showReject = ref(false);
  const rejectRemark = ref('');
  const rejectLoading = ref(false);
  const rejectTarget = ref<RegisterAuditItem | null>(null);

  function openReject(row: RegisterAuditItem) {
    rejectTarget.value = row;
    rejectRemark.value = '';
    showReject.value = true;
  }

  async function confirmReject() {
    if (!rejectTarget.value) return;
    if (!rejectRemark.value.trim()) {
      Message.warning(t('registerAudit.rejectRequired'));
      return;
    }
    rejectLoading.value = true;
    try {
      await registerReject({ id: rejectTarget.value.id, remark: rejectRemark.value.trim() });
      Message.success(t('registerAudit.rejectSuccess'));
      showReject.value = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      rejectLoading.value = false;
    }
  }

  function buildActions(row: RegisterAuditItem): ActionsItem[] {
    const list: ActionsItem[] = [{ label: t('registerAudit.detail'), key: 'detail' }];
    if (row.verifyStatus === 'PENDING') {
      list.push(
        { label: t('registerAudit.approve'), key: 'approve' },
        { label: t('registerAudit.reject'), key: 'reject', danger: true }
      );
    }
    return list;
  }

  function handleActionSelect(row: RegisterAuditItem, key: string) {
    switch (key) {
      case 'detail':
        openDetail(row);
        break;
      case 'approve':
        handleApprove(row);
        break;
      case 'reject':
        openReject(row);
        break;
      default:
        break;
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      fixed: 'left',
      title: t('crmTable.order'),
      width: 50,
      key: SpecialColumnEnum.ORDER,
      resizable: false,
      columnSelectorDisabled: true,
      render: (_row: unknown, rowIndex: number) => rowIndex + 1,
    },
    {
      title: t('registerAudit.name'),
      key: 'name',
      width: 180,
      ellipsis: { tooltip: true },
    },
    {
      title: t('registerAudit.type'),
      key: 'type',
      width: 100,
      render: (row: RegisterAuditItem) => typeLabel(row.type),
    },
    {
      title: t('registerAudit.phone'),
      key: 'phone',
      width: 130,
    },
    {
      title: t('registerAudit.unifiedSocialCreditCode'),
      key: 'unifiedSocialCreditCode',
      width: 180,
      ellipsis: { tooltip: true },
      render: (row: RegisterAuditItem) => row.unifiedSocialCreditCode || '-',
    },
    {
      title: t('registerAudit.legalPersonName'),
      key: 'legalPersonName',
      width: 120,
      render: (row: RegisterAuditItem) => row.legalPersonName || '-',
    },
    {
      title: t('registerAudit.verifyStatus'),
      key: 'verifyStatus',
      width: 110,
      render: (row: RegisterAuditItem) =>
        h(
          NTag,
          { type: statusTagType(row.verifyStatus), size: 'small' },
          { default: () => statusLabel(row.verifyStatus) }
        ),
    },
    {
      title: t('registerAudit.createTime'),
      key: 'createTime',
      width: 160,
      sortOrder: false,
      sorter: true,
    },
    {
      key: 'operation',
      title: t('common.operation'),
      width: 180,
      fixed: 'right',
      render: (row: RegisterAuditItem) =>
        h(CrmOperationButton, {
          groupList: buildActions(row),
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<RegisterAuditItem>(registerPageList, {
    tableKey: TableKeyEnum.SYSTEM_REGISTER_AUDIT_TABLE,
    columns,
    showSetting: false,
    containerClass: '.crm-register-audit-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  watch(tableRefreshId, () => {
    loadList();
  });

  function search() {
    const params: Record<string, unknown> = {};
    if (keyword.value.trim()) params.keyword = keyword.value.trim();
    if (queryType.value) params.type = queryType.value;
    if (queryStatus.value) params.verifyStatus = queryStatus.value;
    setLoadListParams(params);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function reset() {
    keyword.value = '';
    queryType.value = '';
    queryStatus.value = '';
    search();
  }

  onMounted(() => {
    search();
  });
</script>

<style lang="less" scoped>
  .register-audit-page {
    padding: 16px;
  }
</style>
