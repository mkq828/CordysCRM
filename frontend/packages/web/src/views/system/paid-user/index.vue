<template>
  <div class="paid-user-page flex h-full flex-col overflow-hidden">
    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <n-input
          v-model:value="keyword"
          :placeholder="t('paidUser.keywordPlaceholder')"
          clearable
          class="w-[280px]"
          @keydown.enter="search"
        />
        <CrmSelect
          v-model:value="queryVersion"
          :options="versionOptions"
          :placeholder="t('paidUser.version')"
          clearable
          class="w-[160px]"
        />
        <CrmSelect
          v-model:value="queryStatus"
          :options="statusOptions"
          :placeholder="t('paidUser.status')"
          clearable
          class="w-[160px]"
        />
        <n-button type="primary" @click="search">{{ t('common.search') }}</n-button>
        <n-button class="outline--secondary" @click="reset">{{ t('common.reset') }}</n-button>
        <div class="flex-1" />
        <n-button type="primary" ghost @click="openConfig">{{ t('paidUser.trialSetting') }}</n-button>
      </div>
    </CrmCard>

    <CrmCard no-content-padding hide-footer class="min-h-0 flex-1">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="crm-paid-user-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
      />
    </CrmCard>

    <!-- 续费弹窗 -->
    <n-modal
      v-model:show="showOpen"
      preset="card"
      :title="t('paidUser.renewTitle')"
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

    <!-- 试用设置弹窗 -->
    <n-modal
      v-model:show="showConfig"
      preset="card"
      :title="t('paidUser.trialTitle')"
      class="w-[480px]"
      :mask-closable="false"
    >
      <n-spin :show="configLoading">
        <div class="flex flex-col gap-[16px]">
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('paidUser.freeTrialDays') }}</span>
            <n-input-number v-model:value="configForm.freeTrialDays" :min="1" :max="3650" class="flex-1" />
          </div>
          <div class="flex flex-col gap-[4px]">
            <div class="flex items-center gap-[12px]">
              <span class="w-[100px] shrink-0 text-right">{{ t('paidUser.expireRemindDays') }}</span>
              <n-input v-model:value="configForm.expireRemindDays" class="flex-1" :placeholder="'30,7'" />
            </div>
            <div class="ml-[112px] text-xs text-orange-500">{{ t('paidUser.expireRemindDaysTip') }}</div>
          </div>
          <div class="flex flex-col gap-[4px]">
            <div class="flex items-center gap-[12px]">
              <span class="w-[100px] shrink-0 text-right">{{ t('paidUser.graceDays') }}</span>
              <n-input-number v-model:value="configForm.graceDays" :min="0" :max="365" class="flex-1" />
            </div>
            <div class="ml-[112px] text-xs text-orange-500">{{ t('paidUser.graceDaysTip') }}</div>
          </div>
        </div>
      </n-spin>
      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showConfig = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="configSaving" @click="confirmConfig">
            {{ t('common.confirm') }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- 租户详情弹窗 -->
    <n-modal
      v-model:show="showDetail"
      preset="card"
      :title="t('paidUser.detailTitle')"
      class="w-[720px]"
      :mask-closable="false"
    >
      <n-spin :show="detailLoading">
        <template v-if="detail">
          <n-descriptions label-placement="left" :column="2" bordered size="small" class="mb-[16px]">
            <n-descriptions-item :label="t('paidUser.orgName')" :span="2">{{ detail.orgName }}</n-descriptions-item>
            <n-descriptions-item :label="t('paidUser.orgType')">{{ orgTypeLabel(detail.orgType) }}</n-descriptions-item>
            <n-descriptions-item :label="t('paidUser.adminName')">{{ detail.adminName || '-' }}</n-descriptions-item>
            <n-descriptions-item :label="t('paidUser.phone')">{{ detail.phone || '-' }}</n-descriptions-item>
            <n-descriptions-item :label="t('paidUser.accountStatus')">{{
              enabledLabel(detail.enabled)
            }}</n-descriptions-item>
            <n-descriptions-item
              v-if="detail.orgType === 'ENTERPRISE'"
              :label="t('paidUser.unifiedSocialCreditCode')"
              :span="2"
            >
              {{ detail.unifiedSocialCreditCode || '-' }}
            </n-descriptions-item>
            <n-descriptions-item
              v-if="detail.orgType === 'ENTERPRISE'"
              :label="t('paidUser.legalPersonName')"
              :span="2"
            >
              {{ detail.legalPersonName || '-' }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('paidUser.version')">
              {{ detail.editionName || (detail.version ? versionLabel(detail.version) : '-') }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('paidUser.status')">
              {{ detail.status ? statusLabel(detail.status) : '-' }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('paidUser.price')">
              {{ detail.price == null ? '-' : `¥${detail.price}` }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('paidUser.expireTime')">{{
              formatTime(detail.expireTime)
            }}</n-descriptions-item>
            <n-descriptions-item :label="t('paidUser.remainingDays')">{{
              remainingDaysLabel(detail)
            }}</n-descriptions-item>
            <n-descriptions-item :label="t('paidUser.aiQuota')">{{ detail.aiQuota ?? '-' }}</n-descriptions-item>
            <n-descriptions-item :label="t('paidUser.aiUsedCalls')">{{
              detail.aiUsedCalls ?? '-'
            }}</n-descriptions-item>
          </n-descriptions>

          <div class="mb-[8px] text-sm font-medium">{{ t('paidUser.history') }}</div>
          <div v-if="!detail.histories?.length" class="text-xs text-[var(--text-n4)]">
            {{ t('paidUser.historyEmpty') }}
          </div>
          <div v-else class="flex flex-col gap-[8px]">
            <div
              v-for="(h, i) in detail.histories"
              :key="i"
              class="flex items-center gap-[12px] rounded border border-[var(--divider-color)] px-[12px] py-[8px] text-xs"
            >
              <n-tag :type="h.action === 'UPGRADE' ? 'warning' : 'info'" size="small">
                {{ h.action === 'UPGRADE' ? t('paidUser.historyUpgrade') : t('paidUser.historyOpen') }}
              </n-tag>
              <span>{{ h.fromVersion ? `${h.fromVersion} → ${h.toVersion}` : h.toVersion }}</span>
              <span v-if="h.price != null" class="text-orange-500">¥{{ h.price }}</span>
              <span class="flex-1 text-right text-[var(--text-n4)]">{{ formatTime(h.createTime) }}</span>
            </div>
          </div>
        </template>
      </n-spin>
      <template #footer>
        <div class="flex justify-end">
          <n-button @click="showDetail = false">{{ t('common.close') }}</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { h, ref, watch } from 'vue';
  import {
    NButton,
    NDescriptions,
    NDescriptionsItem,
    NInput,
    NInputNumber,
    NModal,
    NSpin,
    NTag,
    useMessage,
  } from 'naive-ui';
  import dayjs from 'dayjs';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { Edition } from '@lib/shared/models/system/edition';
  import type {
    TenantPlanDetail,
    TenantPlanItem,
    TenantPlanStatus,
    TenantPlanVersion,
  } from '@lib/shared/models/system/tenant-plan';

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
    tenantPlanDetail,
    tenantPlanGetConfig,
    tenantPlanOpen,
    tenantPlanPageList,
    tenantPlanToggle,
    tenantPlanToggleDemo,
    tenantPlanUpdateConfig,
    tenantPlanUpgrade,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const keyword = ref('');
  const queryVersion = ref<TenantPlanVersion | ''>('');
  const queryStatus = ref<TenantPlanStatus | ''>('');
  const tableRefreshId = ref(0);

  const editionMap = ref<Record<string, string>>({});
  const versionOptions = ref<{ label: string; value: string }[]>([]);
  const openVersionOptions = ref<{ label: string; value: string }[]>([]);

  const statusOptions = [
    { label: t('common.all'), value: '' },
    { label: t('paidUser.status.active'), value: 'ACTIVE' },
    { label: t('paidUser.status.expired'), value: 'EXPIRED' },
  ];

  async function loadEditions() {
    try {
      const editions = await editionOptions();
      const opts = editions.map((e: Edition) => ({ label: e.name, value: e.code }));
      versionOptions.value = [{ label: t('common.all'), value: '' }, ...opts];
      openVersionOptions.value = opts;
      editions.forEach((e: Edition) => {
        editionMap.value[e.code] = e.name;
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function versionLabel(version: TenantPlanVersion) {
    return editionMap.value[version] ?? version;
  }

  function statusLabel(status: TenantPlanStatus) {
    return t(`paidUser.status.${status.toLowerCase()}`);
  }

  function statusTagType(status: TenantPlanStatus) {
    if (status === 'ACTIVE') return 'success';
    if (status === 'EXPIRED') return 'error';
    return 'warning';
  }

  function formatTime(ts?: number) {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm:ss') : '-';
  }

  function remainingDaysLabel(row: { remainingDays?: number | null }) {
    const d = row.remainingDays;
    if (d == null) return '-';
    if (d <= 0) return t('paidUser.expired');
    return `${d} ${t('paidUser.days')}`;
  }

  function remainingDaysColor(row: TenantPlanItem) {
    const d = row.remainingDays;
    if (d == null) return '';
    if (d <= 0) return 'color: #f5222d';
    if (d <= 7) return 'color: #faad14';
    return '';
  }

  // 开通/续费
  const showOpen = ref(false);
  const openLoading = ref(false);
  const openForm = ref<{ id: string; version: TenantPlanVersion; expireTime: number | null; remark: string }>({
    id: '',
    version: 'BASIC',
    expireTime: null,
    remark: '',
  });

  function openRenew(row: TenantPlanItem) {
    openForm.value = {
      id: row.id,
      version: row.version,
      expireTime: row.expireTime ?? null,
      remark: row.remark || '',
    };
    showOpen.value = true;
  }

  async function confirmOpen() {
    if (!openForm.value.expireTime) {
      Message.warning(t('paidUser.openExpireRequired'));
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
      Message.success(t('paidUser.renewSuccess'));
      showOpen.value = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      openLoading.value = false;
    }
  }

  // 禁用/启用账号
  function handleToggle(row: TenantPlanItem) {
    const willEnable = !row.enabled;
    openModal({
      type: willEnable ? 'default' : 'warning',
      title: willEnable ? t('paidUser.enableTip') : t('paidUser.disableTip'),
      content: willEnable ? t('paidUser.enableTipContent') : t('paidUser.disableTipContent'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await tenantPlanToggle({ organizationId: row.organizationId, enabled: willEnable });
          Message.success(t('paidUser.toggleSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 升级企业版：切换版本 + 按剩余天数补差，成交价列展示补差金额
  function handleUpgrade(row: TenantPlanItem) {
    openModal({
      type: 'warning',
      title: t('paidUser.upgradeTip'),
      content: t('paidUser.upgradeTipContent'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await tenantPlanUpgrade({ id: row.id, editionCode: 'ENTERPRISE' });
          Message.success(t('paidUser.upgradeSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 演示标记：置 sys_organization.is_demo，演示租户不进营收/业绩看板
  function handleToggleDemo(row: TenantPlanItem) {
    const willDemo = !row.demo;
    openModal({
      type: willDemo ? 'warning' : 'default',
      title: willDemo ? t('paidUser.markDemoTip') : t('paidUser.cancelDemoTip'),
      content: willDemo ? t('paidUser.markDemoTipContent') : t('paidUser.cancelDemoTipContent'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await tenantPlanToggleDemo({ organizationId: row.organizationId, demo: willDemo });
          Message.success(t('paidUser.demoToggleSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 租户详情
  const showDetail = ref(false);
  const detailLoading = ref(false);
  const detail = ref<TenantPlanDetail | null>(null);

  async function openDetail(row: TenantPlanItem) {
    showDetail.value = true;
    detailLoading.value = true;
    detail.value = null;
    try {
      detail.value = await tenantPlanDetail({ organizationId: row.organizationId });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      detailLoading.value = false;
    }
  }

  function orgTypeLabel(orgType?: string) {
    return orgType === 'ENTERPRISE' ? t('paidUser.orgType.enterprise') : t('paidUser.orgType.personal');
  }

  function enabledLabel(enabled?: boolean) {
    if (enabled == null) return '-';
    return enabled ? t('paidUser.accountEnabled') : t('paidUser.accountDisabled');
  }

  function buildActions(row: TenantPlanItem): ActionsItem[] {
    const list: ActionsItem[] = [
      { label: t('paidUser.detail'), key: 'detail' },
      { label: t('paidUser.renew'), key: 'renew' },
    ];
    if (row.version !== 'ENTERPRISE') {
      list.push({ label: t('paidUser.upgrade'), key: 'upgrade' });
    }
    if (row.enabled !== null && row.enabled !== undefined) {
      list.push(
        row.enabled
          ? { label: t('paidUser.disable'), key: 'disable', danger: true }
          : { label: t('paidUser.enable'), key: 'enable' }
      );
    }
    list.push(
      row.demo
        ? { label: t('paidUser.cancelDemo'), key: 'cancelDemo' }
        : { label: t('paidUser.markDemo'), key: 'markDemo' }
    );
    return list;
  }

  function handleActionSelect(row: TenantPlanItem, key: string) {
    switch (key) {
      case 'detail':
        openDetail(row);
        break;
      case 'renew':
        openRenew(row);
        break;
      case 'upgrade':
        handleUpgrade(row);
        break;
      case 'enable':
      case 'disable':
        handleToggle(row);
        break;
      case 'markDemo':
      case 'cancelDemo':
        handleToggleDemo(row);
        break;
      default:
        break;
    }
  }

  // 试用设置
  const showConfig = ref(false);
  const configLoading = ref(false);
  const configSaving = ref(false);
  const configForm = ref<{ freeTrialDays: number | null; expireRemindDays: string; graceDays: number | null }>({
    freeTrialDays: null,
    expireRemindDays: '',
    graceDays: null,
  });

  async function openConfig() {
    showConfig.value = true;
    configLoading.value = true;
    try {
      const cfg = await tenantPlanGetConfig();
      configForm.value = {
        freeTrialDays: cfg.freeTrialDays ?? null,
        expireRemindDays: cfg.expireRemindDays || '',
        graceDays: cfg.graceDays ?? null,
      };
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      configLoading.value = false;
    }
  }

  async function confirmConfig() {
    if (configForm.value.freeTrialDays == null) {
      Message.warning(t('paidUser.freeTrialDaysRequired'));
      return;
    }
    configSaving.value = true;
    try {
      await tenantPlanUpdateConfig({
        freeTrialDays: configForm.value.freeTrialDays,
        expireRemindDays: configForm.value.expireRemindDays.trim(),
        graceDays: configForm.value.graceDays ?? undefined,
      });
      Message.success(t('paidUser.configSaveSuccess'));
      showConfig.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      configSaving.value = false;
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
      title: t('paidUser.orgName'),
      key: 'orgName',
      width: 180,
      ellipsis: { tooltip: true },
    },
    {
      title: t('paidUser.version'),
      key: 'version',
      width: 100,
      render: (row: TenantPlanItem) => versionLabel(row.version),
    },
    {
      title: t('paidUser.price'),
      key: 'price',
      width: 110,
      render: (row: TenantPlanItem) => (row.price == null ? '-' : `¥${row.price}`),
    },
    {
      title: t('paidUser.phone'),
      key: 'phone',
      width: 130,
      render: (row: TenantPlanItem) => row.phone || '-',
    },
    {
      title: t('paidUser.accountStatus'),
      key: 'enabled',
      width: 100,
      render: (row: TenantPlanItem) =>
        row.enabled == null
          ? '-'
          : h(
              NTag,
              { type: row.enabled ? 'success' : 'error', size: 'small' },
              {
                default: () => (row.enabled ? t('paidUser.accountEnabled') : t('paidUser.accountDisabled')),
              }
            ),
    },
    {
      title: t('paidUser.status'),
      key: 'status',
      width: 100,
      render: (row: TenantPlanItem) =>
        h(NTag, { type: statusTagType(row.status), size: 'small' }, { default: () => statusLabel(row.status) }),
    },
    {
      title: t('paidUser.demo'),
      key: 'demo',
      width: 80,
      render: (row: TenantPlanItem) =>
        h(
          NTag,
          { type: row.demo ? 'warning' : 'default', size: 'small' },
          { default: () => (row.demo ? t('paidUser.demoYes') : t('paidUser.demoNo')) }
        ),
    },
    {
      title: t('paidUser.expireTime'),
      key: 'expireTime',
      width: 160,
      render: (row: TenantPlanItem) => formatTime(row.expireTime),
    },
    {
      title: t('paidUser.remainingDays'),
      key: 'remainingDays',
      width: 110,
      render: (row: TenantPlanItem) =>
        h('span', { style: remainingDaysColor(row) }, { default: () => remainingDaysLabel(row) }),
    },
    {
      title: t('paidUser.lastLoginTime'),
      key: 'lastLoginTime',
      width: 160,
      render: (row: TenantPlanItem) => formatTime(row.lastLoginTime),
    },
    {
      title: t('paidUser.createTime'),
      key: 'createTime',
      width: 160,
      sortOrder: false,
      sorter: true,
      render: (row: TenantPlanItem) => formatTime(row.createTime),
    },
    {
      key: 'operation',
      title: t('common.operation'),
      width: 220,
      fixed: 'right',
      render: (row: TenantPlanItem) =>
        h(CrmOperationButton, {
          groupList: buildActions(row),
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<TenantPlanItem>(tenantPlanPageList, {
    tableKey: TableKeyEnum.SYSTEM_PAID_USER_TABLE,
    columns,
    showSetting: true,
    containerClass: '.crm-paid-user-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  watch(tableRefreshId, () => {
    loadList();
  });

  function search() {
    const params: Record<string, unknown> = {};
    if (keyword.value.trim()) params.keyword = keyword.value.trim();
    if (queryVersion.value) params.version = queryVersion.value;
    if (queryStatus.value) params.status = queryStatus.value;
    setLoadListParams(params);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function reset() {
    keyword.value = '';
    queryVersion.value = '';
    queryStatus.value = '';
    search();
  }

  onMounted(() => {
    loadEditions();
    search();
  });
</script>

<style lang="less" scoped>
  .paid-user-page {
    @apply flex h-full flex-col overflow-hidden;
  }
</style>
