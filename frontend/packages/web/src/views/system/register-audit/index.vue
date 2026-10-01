<template>
  <div class="register-audit-page flex h-full flex-col overflow-hidden">
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

    <CrmCard
      no-content-padding
      hide-footer
      :special-height="licenseStore.expiredDuring ? 272 : 0"
      class="min-h-0 flex-1"
    >
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="crm-register-audit-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
        @refresh="propsEvent.refresh"
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
          <n-descriptions-item :label="t('registerAudit.remainDays')" :span="2">
            {{ detail.remainDays == null ? '-' : detail.remainDays }}
          </n-descriptions-item>
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
      <template #footer>
        <template v-if="detail?.verifyStatus === 'PENDING'">
          <div class="flex w-full flex-col gap-[12px]">
            <div class="flex items-start gap-[8px]">
              <span class="w-[72px] shrink-0 pt-[6px] text-right text-[13px] text-[var(--text-n3)]">{{
                t('registerAudit.verifyRemark')
              }}</span>
              <n-input
                v-model:value="verifyRemark"
                type="textarea"
                :rows="2"
                maxlength="255"
                show-count
                class="flex-1"
                :placeholder="t('registerAudit.verifyRemarkPlaceholder')"
              />
            </div>
            <div class="flex w-full items-center justify-end gap-[12px]">
              <n-button secondary @click="showDetail = false">{{ t('common.cancel') }}</n-button>
              <n-button type="error" :loading="rejectLoading" @click="rejectFromDetail">{{
                t('registerAudit.reject')
              }}</n-button>
              <n-button type="primary" :loading="approveLoading" @click="approveFromDetail">{{
                t('registerAudit.approve')
              }}</n-button>
            </div>
          </div>
        </template>
        <div v-else class="flex justify-end">
          <n-button @click="showDetail = false">{{ t('common.close') }}</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 开通套餐弹窗 -->
    <n-modal
      v-model:show="showOpen"
      preset="card"
      :title="t('registerAudit.openTitle')"
      class="w-[480px]"
      :mask-closable="false"
    >
      <div class="flex flex-col gap-[16px]">
        <div class="flex items-center gap-[12px]">
          <span class="w-[80px] shrink-0 text-right">{{ t('paidUser.version') }}</span>
          <CrmSelect v-model:value="openForm.version" :options="openVersionOptions" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[80px] shrink-0 text-right">{{ t('paidUser.expireTime') }}</span>
          <CrmDatePicker v-model:value="openForm.expireTime" type="datetime" class="flex-1" />
        </div>
        <div class="flex items-start gap-[12px]">
          <span class="w-[80px] shrink-0 pt-[6px] text-right">{{ t('paidUser.remark') }}</span>
          <n-input
            v-model:value="openForm.remark"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-count
            class="flex-1"
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showOpen = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="openLoading" @click="confirmOpen">
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
  import type { Edition } from '@lib/shared/models/system/edition';
  import type { RegisterAuditItem, RegisterType, RegisterVerifyStatus } from '@lib/shared/models/system/register';
  import type { TenantPlanVersion } from '@lib/shared/models/system/tenant-plan';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDatePicker from '@/components/pure/crm-date-picker/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSelect from '@/components/pure/crm-select/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import {
    editionOptions,
    registerApprove,
    registerDetail,
    registerPageList,
    registerReject,
    registerToggle,
    tenantPlanOpen,
  } from '@/api/modules';
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
    { label: t('common.all'), value: '' },
    { label: t('registerAudit.type.personal'), value: 'PERSONAL' },
    { label: t('registerAudit.type.enterprise'), value: 'ENTERPRISE' },
  ]);

  const statusOptions = computed(() => [
    { label: t('common.all'), value: '' },
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

  // 审核备注（通过选填、拒绝必填）
  const verifyRemark = ref('');
  const approveLoading = ref(false);
  const rejectLoading = ref(false);

  async function openDetail(row: RegisterAuditItem) {
    showDetail.value = true;
    detailLoading.value = true;
    detail.value = null;
    verifyRemark.value = '';
    try {
      detail.value = await registerDetail(row.id);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      detailLoading.value = false;
    }
  }

  // 审核通过（详情弹窗内点「通过」，备注选填）
  async function approveFromDetail() {
    if (!detail.value) return;
    approveLoading.value = true;
    try {
      await registerApprove({ id: detail.value.id, remark: verifyRemark.value.trim() || undefined });
      Message.success(t('registerAudit.approveSuccess'));
      showDetail.value = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      approveLoading.value = false;
    }
  }

  // 审核拒绝（详情弹窗内点「拒绝」，备注必填）
  async function rejectFromDetail() {
    if (!detail.value) return;
    if (!verifyRemark.value.trim()) {
      Message.warning(t('registerAudit.verifyRemarkRequired'));
      return;
    }
    rejectLoading.value = true;
    try {
      await registerReject({ id: detail.value.id, remark: verifyRemark.value.trim() });
      Message.success(t('registerAudit.rejectSuccess'));
      showDetail.value = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      rejectLoading.value = false;
    }
  }

  function handleToggle(row: RegisterAuditItem) {
    const willEnable = !row.enabled;
    openModal({
      type: willEnable ? 'default' : 'warning',
      title: willEnable ? t('registerAudit.enableTip') : t('registerAudit.disableTip'),
      content: willEnable ? t('registerAudit.enableTipContent') : t('registerAudit.disableTipContent'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await registerToggle({ id: row.id, enabled: willEnable });
          Message.success(t('registerAudit.toggleSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 开通套餐
  const showOpen = ref(false);
  const openLoading = ref(false);
  const openForm = ref<{ id: string; version: TenantPlanVersion; expireTime: number | null; remark: string }>({
    id: '',
    version: 'BASIC',
    expireTime: null,
    remark: '',
  });

  const openVersionOptions = ref<{ label: string; value: string }[]>([]);

  async function loadEditions() {
    try {
      const editions = await editionOptions();
      openVersionOptions.value = editions.map((e: Edition) => ({ label: e.name, value: e.code }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function openPlan(row: RegisterAuditItem) {
    openForm.value = {
      id: row.planId || '',
      version: 'BASIC',
      expireTime: Date.now() + 365 * 24 * 60 * 60 * 1000,
      remark: '',
    };
    showOpen.value = true;
  }

  async function confirmOpen() {
    if (!openForm.value.expireTime) {
      Message.warning(t('registerAudit.openExpireRequired'));
      return;
    }
    openLoading.value = true;
    try {
      await tenantPlanOpen({
        id: openForm.value.id,
        version: openForm.value.version,
        expireTime: openForm.value.expireTime,
        remark: openForm.value.remark.trim(),
      });
      Message.success(t('registerAudit.openSuccess'));
      showOpen.value = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      openLoading.value = false;
    }
  }

  function buildActions(row: RegisterAuditItem): ActionsItem[] {
    const list: ActionsItem[] = [];
    if (row.verifyStatus === 'PENDING') {
      list.push({ label: t('registerAudit.audit'), key: 'audit' });
    } else {
      list.push({ label: t('registerAudit.detail'), key: 'detail' });
    }
    if (row.planStatus === 'FREE') {
      list.push({ label: t('registerAudit.open'), key: 'open' });
    }
    if (row.enabled !== null && row.enabled !== undefined) {
      list.push(
        row.enabled
          ? { label: t('registerAudit.disable'), key: 'disable', danger: true }
          : { label: t('registerAudit.enable'), key: 'enable' }
      );
    }
    return list;
  }

  function handleActionSelect(row: RegisterAuditItem, key: string) {
    switch (key) {
      case 'detail':
      case 'audit':
        openDetail(row);
        break;
      case 'open':
        openPlan(row);
        break;
      case 'enable':
      case 'disable':
        handleToggle(row);
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
      title: t('registerAudit.planStatus'),
      key: 'planStatus',
      width: 100,
      render: (row: RegisterAuditItem) =>
        row.planStatus === 'FREE'
          ? h(NTag, { type: 'warning', size: 'small' }, { default: () => t('registerAudit.planStatus.free') })
          : '-',
    },
    {
      title: t('registerAudit.remainDays'),
      key: 'remainDays',
      width: 120,
      render: (row: RegisterAuditItem) => (row.remainDays == null ? '-' : row.remainDays),
    },
    {
      title: t('registerAudit.usageDays'),
      key: 'usageDays',
      width: 120,
      render: (row: RegisterAuditItem) => (row.usageDays == null ? '-' : row.usageDays),
    },
    {
      title: t('registerAudit.lastLoginTime'),
      key: 'lastLoginTime',
      width: 160,
      render: (row: RegisterAuditItem) => formatTime(row.lastLoginTime),
    },
    {
      title: t('registerAudit.accountStatus'),
      key: 'enabled',
      width: 100,
      render: (row: RegisterAuditItem) =>
        row.enabled == null
          ? '-'
          : h(
              NTag,
              { type: row.enabled ? 'success' : 'error', size: 'small' },
              {
                default: () => (row.enabled ? t('registerAudit.accountEnabled') : t('registerAudit.accountDisabled')),
              }
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
    showSetting: true,
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
    loadEditions();
    search();
  });
</script>

<style lang="less" scoped>
  .register-audit-page {
    @apply flex h-full flex-col overflow-hidden;
  }
</style>
