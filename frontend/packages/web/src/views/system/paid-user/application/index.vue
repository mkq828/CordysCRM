<template>
  <div class="plan-application-page flex h-full flex-col gap-[16px] overflow-hidden">
    <CrmCard hide-footer auto-height>
      <div class="flex items-center gap-[12px]">
        <n-input
          v-model:value="keyword"
          :placeholder="t('planApplication.keywordPlaceholder')"
          clearable
          class="w-[240px]"
          @keydown.enter="search"
        />
        <CrmSelect
          v-model:value="queryStatus"
          :options="statusOptions"
          :placeholder="t('planApplication.status')"
          clearable
          class="w-[160px]"
        />
        <n-button type="primary" @click="search">{{ t('common.search') }}</n-button>
        <n-button class="outline--secondary" @click="reset">{{ t('common.reset') }}</n-button>
      </div>
    </CrmCard>

    <CrmCard hide-footer auto-height class="min-h-0 flex-1">
      <n-data-table
        :columns="columns"
        :data="list"
        :loading="loading"
        :bordered="false"
        :single-line="false"
        size="small"
        :scroll-x="960"
      />
      <div v-if="!list.length && !loading" class="py-[40px] text-center text-[var(--text-n4)]">
        {{ t('planApplication.empty') }}
      </div>
      <div class="flex justify-end pt-[16px]">
        <n-pagination
          v-model:page="current"
          :page-size="pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update:page="loadData"
          @update:page-size="onPageSizeChange"
        />
      </div>
    </CrmCard>
  </div>
  <ApplicationReviewModal v-model:show="reviewVisible" :application="reviewRow" @success="handleReviewSuccess" />
</template>

<script setup lang="ts">
  import { h, onMounted, ref } from 'vue';
  import { NButton, NDataTable, NImage, NInput, NPagination, NTag } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { TenantPlanApplicationItem, TenantPlanApplicationStatus } from '@lib/shared/models/system/tenant-plan';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmSelect from '@/components/pure/crm-select/index.vue';
  import ApplicationReviewModal from './applicationReviewModal.vue';

  import { tenantPlanApplicationList } from '@/api/modules';
  import { useUserStore } from '@/store';
  import useAppStore from '@/store/modules/app';

  import type { DataTableColumns } from 'naive-ui';

  const { t } = useI18n();
  const userStore = useUserStore();
  const appStore = useAppStore();

  const list = ref<TenantPlanApplicationItem[]>([]);
  const loading = ref(false);
  const keyword = ref('');
  const queryStatus = ref<TenantPlanApplicationStatus | ''>('');
  const current = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const reviewVisible = ref(false);
  const reviewRow = ref<TenantPlanApplicationItem | null>(null);

  const statusOptions = [
    { label: t('common.all'), value: '' },
    { label: t('planApplication.status.PENDING'), value: 'PENDING' },
    { label: t('planApplication.status.APPROVED'), value: 'APPROVED' },
    { label: t('planApplication.status.CANCELLED'), value: 'CANCELLED' },
  ];

  async function loadData() {
    loading.value = true;
    try {
      const res = await tenantPlanApplicationList({
        current: current.value,
        pageSize: pageSize.value,
        status: queryStatus.value || undefined,
        keyword: keyword.value.trim() || undefined,
      });
      list.value = res?.list ?? [];
      total.value = res?.total ?? 0;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  function search() {
    current.value = 1;
    loadData();
  }

  // 核销/驳回成功后：刷新列表并同步右上角待核销角标
  function handleReviewSuccess() {
    loadData();
    appStore.initPendingApplicationCount();
  }

  function reset() {
    keyword.value = '';
    queryStatus.value = '';
    search();
  }

  function onPageSizeChange(size: number) {
    pageSize.value = size;
    search();
  }

  function formatTime(ts?: number) {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '-';
  }

  function formatMoney(v?: number | string) {
    const n = Number(v ?? 0);
    return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function versionChange(row: TenantPlanApplicationItem) {
    const from = row.currentVersionName || row.currentVersion || '-';
    const to = row.targetVersionName || row.targetVersion;
    return `${from} → ${to}`;
  }

  function paymentTypeLabel(paymentType: string) {
    return paymentType ? t(`planApplication.paymentType.${paymentType}`) : '-';
  }

  function statusTag(status: string) {
    switch (status) {
      case 'APPROVED':
        return 'success';
      case 'CANCELLED':
        return 'error';
      case 'PENDING':
        return 'warning';
      default:
        return 'default';
    }
  }

  function statusLabel(status: string) {
    return status ? t(`planApplication.status.${status}`) : '-';
  }

  function voucherIds(row: TenantPlanApplicationItem): string[] {
    return (row.voucherIds || '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
  }

  function attachmentUrl(id: string) {
    return `${PreviewAttachmentUrl}/${id}?userId=${userStore.userInfo.id}`;
  }

  function openReview(row: TenantPlanApplicationItem) {
    reviewRow.value = row;
    reviewVisible.value = true;
  }

  const columns: DataTableColumns<TenantPlanApplicationItem> = [
    { title: t('planApplication.orgName'), key: 'orgName', width: 160, ellipsis: { tooltip: true } },
    {
      title: t('planApplication.versionChange'),
      key: 'versionChange',
      width: 180,
      render: (row) => versionChange(row),
    },
    {
      title: t('planApplication.amount'),
      key: 'amount',
      width: 110,
      render: (row) => (row.amount == null ? '-' : `¥${formatMoney(row.amount)}`),
    },
    {
      title: t('planApplication.validityDays'),
      key: 'validityDays',
      width: 90,
      render: (row) => (row.validityDays == null ? '-' : `${row.validityDays}${t('paidUser.days')}`),
    },
    {
      title: t('planApplication.paymentType'),
      key: 'paymentType',
      width: 100,
      render: (row) => paymentTypeLabel(row.paymentType),
    },
    {
      title: t('planApplication.voucher'),
      key: 'voucher',
      width: 140,
      render: (row) => {
        const ids = voucherIds(row);
        if (!ids.length) return '-';
        return h(
          'div',
          { class: 'flex gap-[6px]' },
          ids.map((id) =>
            h(NImage, {
              src: attachmentUrl(id),
              width: 40,
              height: 40,
              objectFit: 'cover',
              class: 'rounded-[4px]',
            })
          )
        );
      },
    },
    {
      title: t('planApplication.status'),
      key: 'status',
      width: 90,
      render: (row) =>
        h(NTag, { type: statusTag(row.status), size: 'small' }, { default: () => statusLabel(row.status) }),
    },
    {
      title: t('planApplication.createTime'),
      key: 'createTime',
      width: 150,
      render: (row) => formatTime(row.createTime),
    },
    {
      title: t('common.operation'),
      key: 'operation',
      width: 130,
      fixed: 'right',
      render: (row) => {
        if (row.status !== 'PENDING') {
          return h('span', { class: 'text-[var(--text-n4)]' }, '-');
        }
        return h('div', { class: 'flex gap-[8px]' }, [
          h(
            NButton,
            { text: true, type: 'primary', size: 'small', onClick: () => openReview(row) },
            { default: () => t('planApplication.approve') }
          ),
          h(
            NButton,
            { text: true, type: 'error', size: 'small', onClick: () => openReview(row) },
            { default: () => t('planApplication.cancel') }
          ),
        ]);
      },
    },
  ];

  onMounted(() => {
    loadData();
  });
</script>

<style lang="less" scoped>
  .plan-application-page {
    @apply flex h-full flex-col overflow-hidden;
  }
</style>
