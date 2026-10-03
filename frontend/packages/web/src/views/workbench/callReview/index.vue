<template>
  <CrmCard hide-footer no-content-padding>
    <div class="h-full px-[24px] pt-[24px]">
      <div class="mb-[16px] flex items-center gap-[8px]">
        <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
        <span class="text-[14px] font-semibold">{{ t('workbench.callReview.title') }}</span>
      </div>
      <div class="mb-[12px] text-[13px] text-orange-500">{{ t('workbench.callReview.desc') }}</div>

      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="call-review-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
        @refresh="searchData"
      >
        <template #tableTop>
          <div class="flex items-center gap-[12px]">
            <n-button type="primary" @click="uploadVisible = true">
              <template #icon>
                <CrmIcon type="iconicon_upload" :size="16" />
              </template>
              {{ t('workbench.callReview.upload') }}
            </n-button>
            <n-button class="n-btn-outline-primary" type="primary" ghost @click="openConfig">
              <template #icon>
                <CrmIcon type="iconicon_set_up" :size="16" />
              </template>
              {{ t('workbench.callReview.config') }}
            </n-button>
          </div>
        </template>
        <template #actionRight>
          <n-select
            v-model:value="statusFilter"
            clearable
            :placeholder="t('workbench.callReview.status')"
            :options="statusOptions"
            class="w-[140px]"
            @update:value="handleStatusChange"
          />
        </template>
      </CrmTable>
    </div>
  </CrmCard>

  <CallReviewDrawer v-model:visible="detailVisible" :record-id="detailId" @refresh="handleDrawerRefresh" />
  <CallReviewUploadModal v-model:visible="uploadVisible" @saved="handleUploaded" />
  <CallReviewConfigModal v-model:visible="configVisible" />
</template>

<script setup lang="ts">
  import { computed, h, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import { NButton, NSelect, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CallReviewResponse, CallReviewStatus } from '@lib/shared/models/callReview';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CallReviewConfigModal from './components/callReviewConfigModal.vue';
  import CallReviewDrawer from './components/callReviewDrawer.vue';
  import CallReviewUploadModal from './components/callReviewUploadModal.vue';

  import { getCallReviewPage, retryCallReview } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const TRANSIENT_STATUSES: CallReviewStatus[] = ['PENDING_TRANSCRIBE', 'TRANSCRIBING', 'ANALYZING'];

  const statusMetaMap: Record<CallReviewStatus, { type: 'info' | 'success' | 'warning' | 'error' }> = {
    PENDING_TRANSCRIBE: { type: 'warning' },
    TRANSCRIBING: { type: 'warning' },
    ANALYZING: { type: 'warning' },
    DONE: { type: 'success' },
    FAILED: { type: 'error' },
  };

  const statusOptions = computed(() =>
    (Object.keys(statusMetaMap) as CallReviewStatus[]).map((status) => ({
      label: t(`workbench.callReview.status.${status}`),
      value: status,
    }))
  );

  function formatTime(value?: number | string): string {
    if (!value) {
      return '-';
    }
    if (typeof value === 'string') {
      return value;
    }
    return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
  }

  function formatDuration(value?: number): string {
    return value === null || value === undefined ? '-' : `${value}s`;
  }

  function getStatusLabel(status: CallReviewStatus): string {
    return t(`workbench.callReview.status.${status}`);
  }

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const statusFilter = ref<string | null>(null);

  const detailVisible = ref(false);
  const detailId = ref('');
  const uploadVisible = ref(false);
  const configVisible = ref(false);
  const refreshTick = ref(0);

  function openDetail(row: CallReviewResponse) {
    detailId.value = row.id;
    detailVisible.value = true;
  }

  async function handleRetry(row: CallReviewResponse) {
    try {
      await retryCallReview(row.id);
      Message.success(t('workbench.callReview.retrySuccess'));
      refreshTick.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function handleActionSelect(row: CallReviewResponse, key: string) {
    if (key === 'detail') {
      openDetail(row);
    } else if (key === 'retry') {
      handleRetry(row);
    }
  }

  const columns: CrmDataTableColumn<CallReviewResponse>[] = [
    {
      fixed: 'left',
      title: t('crmTable.order'),
      width: 50,
      key: SpecialColumnEnum.ORDER,
      resizable: false,
      columnSelectorDisabled: true,
      render: (_row, index) => index + 1,
    },
    {
      title: t('workbench.callReview.callTime'),
      key: 'callTime',
      width: 170,
      ellipsis: { tooltip: true },
      render: (row) => formatTime(row.callTime),
    },
    {
      title: t('workbench.callReview.caller'),
      key: 'caller',
      width: 140,
      ellipsis: { tooltip: true },
      render: (row) => row.caller || '-',
    },
    {
      title: t('workbench.callReview.callee'),
      key: 'callee',
      width: 140,
      ellipsis: { tooltip: true },
      render: (row) => row.callee || '-',
    },
    {
      title: t('workbench.callReview.customer'),
      key: 'customerName',
      width: 140,
      ellipsis: { tooltip: true },
      render: (row) => row.customerName || row.customerPhone || '-',
    },
    {
      title: t('workbench.callReview.duration'),
      key: 'duration',
      width: 100,
      render: (row) => formatDuration(row.duration),
    },
    {
      title: t('workbench.callReview.status'),
      key: 'status',
      width: 110,
      render: (row) =>
        h(
          CrmTag,
          {
            size: 'small',
            theme: 'light',
            type: statusMetaMap[row.status]?.type ?? 'info',
            tooltipDisabled: true,
          },
          { default: () => getStatusLabel(row.status) }
        ),
    },
    {
      title: t('common.operation'),
      key: 'operation',
      width: 120,
      fixed: 'right',
      render: (row) =>
        h(CrmOperationButton, {
          groupList: [
            { label: t('workbench.callReview.detail'), key: 'detail' },
            ...(row.status === 'FAILED' ? [{ label: t('workbench.callReview.retry'), key: 'retry' }] : []),
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<CallReviewResponse>(getCallReviewPage, {
    columns,
    tableKey: TableKeyEnum.AI_CALL_REVIEW,
    permission: [],
    showSetting: true,
    containerClass: '.call-review-table',
  });

  function searchData() {
    setLoadListParams({ status: statusFilter.value || undefined });
    loadList(false);
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function handleStatusChange() {
    searchData();
  }

  function openConfig() {
    configVisible.value = true;
  }

  // 存在转写中/分析中的记录时，静默轮询列表刷新状态
  let pollTimer: ReturnType<typeof setInterval> | undefined;

  function hasTransient(): boolean {
    return (propsRes.value.data || []).some((item) => TRANSIENT_STATUSES.includes(item.status));
  }

  async function silentRefresh() {
    const res = await getCallReviewPage({
      current: propsRes.value.crmPagination?.page ?? 1,
      pageSize: propsRes.value.crmPagination?.pageSize ?? 10,
      status: statusFilter.value || undefined,
    });
    propsRes.value.data = res.list as unknown as typeof propsRes.value.data;
    if (propsRes.value.crmPagination) {
      propsRes.value.crmPagination.itemCount = res.total;
    }
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = undefined;
    }
  }

  function ensurePolling() {
    if (pollTimer) {
      return;
    }
    pollTimer = setInterval(async () => {
      if (!hasTransient()) {
        stopPolling();
        return;
      }
      try {
        await silentRefresh();
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      }
    }, 6000);
  }

  function handleUploaded() {
    Message.success(t('workbench.callReview.uploadSuccess'));
    searchData();
    ensurePolling();
  }

  function handleDrawerRefresh() {
    searchData();
    ensurePolling();
  }

  watch(refreshTick, () => {
    searchData();
    ensurePolling();
  });

  onMounted(() => {
    loadList();
    ensurePolling();
  });

  onBeforeUnmount(() => {
    stopPolling();
  });
</script>

<style scoped lang="less">
  .call-review-table :deep(.n-data-table) {
    min-height: 320px;
  }
</style>
