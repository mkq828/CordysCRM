<template>
  <div class="job-exec-log-page flex h-full flex-col overflow-hidden">
    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <n-input
          v-model:value="queryJobName"
          :placeholder="t('jobExecLog.jobNamePlaceholder')"
          clearable
          class="w-[240px]"
          @keydown.enter="search"
        />
        <n-select
          v-model:value="queryStatus"
          :options="statusOptions"
          :placeholder="t('jobExecLog.status')"
          clearable
          class="w-[160px]"
        />
        <CrmDatePicker v-model:value="time" type="datetimerange" class="w-[320px]" />
        <n-button type="primary" @click="search">{{ t('common.search') }}</n-button>
        <n-button class="outline--secondary" @click="reset">{{ t('common.reset') }}</n-button>
      </div>
    </CrmCard>

    <CrmCard no-content-padding hide-footer class="min-h-0 flex-1">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="crm-job-exec-log-table"
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
  import { h, ref } from 'vue';
  import { NInput, NSelect, NTag } from 'naive-ui';
  import dayjs from 'dayjs';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { JobExecLogItem, JobExecLogListParams } from '@lib/shared/models/system/jobExecLog';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDatePicker from '@/components/pure/crm-date-picker/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { jobExecLogList } from '@/api/modules/system/jobExecLog';

  const { t } = useI18n();

  const STATUS_TYPES = ['RUNNING', 'SUCCESS', 'FAILED'];
  const statusOptions = STATUS_TYPES.map((v) => ({ label: t(`jobExecLog.status.${v}`), value: v }));

  const queryJobName = ref('');
  const queryStatus = ref<string>('');
  const time = ref<[number, number] | null>(null);

  function formatTime(ts?: number | null) {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm:ss') : '-';
  }

  function formatDuration(ms?: number | null) {
    if (ms == null) return '-';
    if (ms < 1000) return `${ms} ms`;
    if (ms < 60000) return `${(ms / 1000).toFixed(1)} s`;
    const min = Math.floor(ms / 60000);
    const sec = Math.round((ms % 60000) / 1000);
    return `${min} min ${sec} s`;
  }

  function statusTagType(status: string) {
    if (status === 'SUCCESS') return 'success';
    if (status === 'FAILED') return 'error';
    return 'warning';
  }

  function statusLabel(status: string) {
    const key = `jobExecLog.status.${status}`;
    const label = t(key);
    return label === key ? status : label;
  }

  function buildFilterParams(): JobExecLogListParams {
    const params: JobExecLogListParams = {};
    if (queryJobName.value.trim()) params.jobName = queryJobName.value.trim();
    if (queryStatus.value) params.status = queryStatus.value;
    if (time.value) {
      const [startTime, endTime] = time.value;
      params.startTime = startTime;
      params.endTime = endTime;
    }
    return params;
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
      title: t('jobExecLog.startTime'),
      key: 'startTime',
      width: 170,
      render: (row: JobExecLogItem) => formatTime(row.startTime),
    },
    {
      title: t('jobExecLog.jobName'),
      key: 'jobName',
      width: 220,
      ellipsis: { tooltip: true },
      render: (row: JobExecLogItem) => row.jobName || '-',
    },
    {
      title: t('jobExecLog.status'),
      key: 'status',
      width: 110,
      render: (row: JobExecLogItem) =>
        h(NTag, { type: statusTagType(row.status), size: 'small' }, { default: () => statusLabel(row.status) }),
    },
    {
      title: t('jobExecLog.duration'),
      key: 'durationMs',
      width: 120,
      render: (row: JobExecLogItem) => formatDuration(row.durationMs),
    },
    {
      title: t('jobExecLog.errorMsg'),
      key: 'errorMsg',
      minWidth: 200,
      ellipsis: { tooltip: true },
      render: (row: JobExecLogItem) => row.errorMsg || '-',
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<JobExecLogItem>(jobExecLogList, {
    tableKey: TableKeyEnum.SYSTEM_JOB_EXEC_LOG_TABLE,
    columns,
    showSetting: true,
    containerClass: '.crm-job-exec-log-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  function search() {
    setLoadListParams(buildFilterParams());
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function reset() {
    queryJobName.value = '';
    queryStatus.value = '';
    time.value = null;
    search();
  }

  onMounted(() => {
    search();
  });
</script>

<style lang="less" scoped>
  .job-exec-log-page {
    @apply flex h-full flex-col overflow-hidden;
  }
</style>
