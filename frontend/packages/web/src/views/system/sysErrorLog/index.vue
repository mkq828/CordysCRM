<template>
  <div class="sys-error-log-page flex h-full flex-col overflow-hidden">
    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <n-input
          v-model:value="queryLoggerName"
          :placeholder="t('sysErrorLog.loggerNamePlaceholder')"
          clearable
          class="w-[200px]"
          @keydown.enter="search"
        />
        <n-input
          v-model:value="queryTraceId"
          :placeholder="t('sysErrorLog.traceIdPlaceholder')"
          clearable
          class="w-[200px]"
          @keydown.enter="search"
        />
        <n-input
          v-model:value="queryKeyword"
          :placeholder="t('sysErrorLog.keywordPlaceholder')"
          clearable
          class="w-[200px]"
          @keydown.enter="search"
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
        class="crm-sys-error-log-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
        @refresh="propsEvent.refresh"
      />
    </CrmCard>

    <n-modal v-model:show="showStack" preset="card" :title="t('sysErrorLog.stackTrace')" class="!w-[760px]">
      <pre class="stack-pre">{{ currentStack || '-' }}</pre>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { h, ref } from 'vue';
  import { NButton, NInput, NModal } from 'naive-ui';
  import dayjs from 'dayjs';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { SysErrorLogItem, SysErrorLogListParams } from '@lib/shared/models/system/sysErrorLog';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDatePicker from '@/components/pure/crm-date-picker/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { sysErrorLogList } from '@/api/modules/system/sysErrorLog';

  const { t } = useI18n();

  const queryLoggerName = ref('');
  const queryTraceId = ref('');
  const queryKeyword = ref('');
  const time = ref<[number, number] | null>(null);

  const showStack = ref(false);
  const currentStack = ref('');

  function formatTime(ts?: number | null) {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm:ss') : '-';
  }

  function viewStack(row: SysErrorLogItem) {
    currentStack.value = row.stackTrace || row.message || '-';
    showStack.value = true;
  }

  function buildFilterParams(): SysErrorLogListParams {
    const params: SysErrorLogListParams = {};
    if (queryLoggerName.value.trim()) params.loggerName = queryLoggerName.value.trim();
    if (queryTraceId.value.trim()) params.traceId = queryTraceId.value.trim();
    if (queryKeyword.value.trim()) params.keyword = queryKeyword.value.trim();
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
      title: t('sysErrorLog.createTime'),
      key: 'createTime',
      width: 170,
      render: (row: SysErrorLogItem) => formatTime(row.createTime),
    },
    {
      title: t('sysErrorLog.loggerName'),
      key: 'loggerName',
      width: 240,
      ellipsis: { tooltip: true },
      render: (row: SysErrorLogItem) => row.loggerName || '-',
    },
    {
      title: t('sysErrorLog.traceId'),
      key: 'traceId',
      width: 150,
      ellipsis: { tooltip: true },
      render: (row: SysErrorLogItem) => row.traceId || '-',
    },
    {
      title: t('sysErrorLog.message'),
      key: 'message',
      minWidth: 240,
      ellipsis: { tooltip: true },
      render: (row: SysErrorLogItem) => row.message || '-',
    },
    {
      title: t('sysErrorLog.operation'),
      key: 'operation',
      width: 100,
      fixed: 'right',
      resizable: false,
      render: (row: SysErrorLogItem) =>
        h(
          NButton,
          { size: 'small', quaternary: true, type: 'primary', onClick: () => viewStack(row) },
          { default: () => t('sysErrorLog.view') }
        ),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<SysErrorLogItem>(sysErrorLogList, {
    tableKey: TableKeyEnum.SYSTEM_ERROR_LOG_TABLE,
    columns,
    showSetting: true,
    containerClass: '.crm-sys-error-log-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  function search() {
    setLoadListParams(buildFilterParams());
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function reset() {
    queryLoggerName.value = '';
    queryTraceId.value = '';
    queryKeyword.value = '';
    time.value = null;
    search();
  }

  onMounted(() => {
    search();
  });
</script>

<style lang="less" scoped>
  .sys-error-log-page {
    @apply flex h-full flex-col overflow-hidden;
  }
  .stack-pre {
    overflow: auto;
    margin: 0;
    max-height: 60vh;
    font-size: 12px;
    white-space: pre-wrap;
    word-break: break-all;
    line-height: 1.6;
  }
</style>
