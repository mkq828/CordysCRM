<template>
  <div class="security-event-page flex h-full flex-col overflow-hidden">
    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <n-select
          v-model:value="queryEventType"
          :options="eventTypeOptions"
          :placeholder="t('securityEvent.eventType')"
          clearable
          class="w-[180px]"
        />
        <n-input
          v-model:value="ip"
          :placeholder="t('securityEvent.ipPlaceholder')"
          clearable
          class="w-[240px]"
          @keydown.enter="search"
        />
        <CrmDatePicker v-model:value="time" type="datetimerange" class="w-[320px]" />
        <n-button type="primary" @click="search">{{ t('common.search') }}</n-button>
        <n-button class="outline--secondary" @click="reset">{{ t('common.reset') }}</n-button>
        <n-button class="outline--secondary" @click="handleExport">{{ t('securityEvent.export') }}</n-button>
      </div>
    </CrmCard>

    <CrmCard no-content-padding hide-footer class="min-h-0 flex-1">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="crm-security-event-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
        @refresh="propsEvent.refresh"
      />
    </CrmCard>

    <CrmDrawer v-model:show="showDetail" :footer="false" :title="t('securityEvent.detailTitle')" :width="720">
      <n-descriptions v-if="detail" label-placement="left" :column="2" bordered size="small">
        <n-descriptions-item :label="t('securityEvent.eventType')">{{
          eventTypeLabel(detail.eventType)
        }}</n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.createTime')">{{
          formatTime(detail.createTime)
        }}</n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.detail')" :span="2">{{
          detail.detail || '-'
        }}</n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.remoteAddr')">{{ detail.remoteAddr || '-' }}</n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.clientIp')">{{ detail.clientIp || '-' }}</n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.forwardChain')" :span="2">{{
          detail.forwardChain || '-'
        }}</n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.ipRegion')">{{ detail.ipRegion || '-' }}</n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.httpStatus')">{{ detail.httpStatus ?? '-' }}</n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.method')">{{ detail.method || '-' }}</n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.traceId')">{{ detail.traceId || '-' }}</n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.uri')" :span="2">
          <span class="break-all">{{ detail.uri || '-' }}</span>
        </n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.userAgent')" :span="2">
          <span class="break-all">{{ detail.userAgent || '-' }}</span>
        </n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.referer')" :span="2">
          <span class="break-all">{{ detail.referer || '-' }}</span>
        </n-descriptions-item>
        <n-descriptions-item :label="t('securityEvent.organizationId')">{{
          detail.organizationId || '-'
        }}</n-descriptions-item>
      </n-descriptions>
    </CrmDrawer>
  </div>
</template>

<script setup lang="ts">
  import { h, ref } from 'vue';
  import { NButton, NDescriptions, NDescriptionsItem, NInput, NSelect, NTag } from 'naive-ui';
  import dayjs from 'dayjs';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { downloadByteFile } from '@lib/shared/method';
  import type { SecurityEventItem, SecurityEventListParams } from '@lib/shared/models/system/securityEvent';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDatePicker from '@/components/pure/crm-date-picker/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { securityEventExport, securityEventList } from '@/api/modules/system/securityEvent';

  const { t } = useI18n();

  const EVENT_TYPES = ['sql_injection', 'xss', 'path_traversal', 'scanner_ua', 'error_flood'];

  const eventTypeOptions = EVENT_TYPES.map((v) => ({ label: t(`securityEvent.eventType.${v}`), value: v }));

  const queryEventType = ref<string>('');
  const ip = ref('');
  const time = ref<[number, number] | null>(null);

  function formatTime(ts?: number) {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm:ss') : '-';
  }

  function eventTypeLabel(type: string) {
    const key = `securityEvent.eventType.${type}`;
    const label = t(key);
    return label === key ? type : label;
  }

  function eventTypeTagType(type: string) {
    return type === 'error_flood' ? 'warning' : 'error';
  }

  function buildFilterParams(): SecurityEventListParams {
    const params: SecurityEventListParams = {};
    if (queryEventType.value) params.eventType = queryEventType.value;
    if (ip.value.trim()) params.ip = ip.value.trim();
    if (time.value) {
      const [startTime, endTime] = time.value;
      params.startTime = startTime;
      params.endTime = endTime;
    }
    return params;
  }

  // 详情
  const showDetail = ref(false);
  const detail = ref<SecurityEventItem | null>(null);

  function openDetail(row: SecurityEventItem) {
    detail.value = row;
    showDetail.value = true;
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
      title: t('securityEvent.createTime'),
      key: 'createTime',
      width: 160,
      render: (row: SecurityEventItem) => formatTime(row.createTime),
    },
    {
      title: t('securityEvent.eventType'),
      key: 'eventType',
      width: 150,
      render: (row: SecurityEventItem) =>
        h(
          NTag,
          { type: eventTypeTagType(row.eventType), size: 'small' },
          { default: () => eventTypeLabel(row.eventType) }
        ),
    },
    {
      title: t('securityEvent.detail'),
      key: 'detail',
      width: 160,
      ellipsis: { tooltip: true },
      render: (row: SecurityEventItem) => row.detail || '-',
    },
    {
      title: t('securityEvent.remoteAddr'),
      key: 'remoteAddr',
      width: 140,
      render: (row: SecurityEventItem) => row.remoteAddr || '-',
    },
    {
      title: t('securityEvent.clientIp'),
      key: 'clientIp',
      width: 140,
      render: (row: SecurityEventItem) => row.clientIp || '-',
    },
    {
      title: t('securityEvent.ipRegion'),
      key: 'ipRegion',
      width: 120,
      render: (row: SecurityEventItem) => row.ipRegion || '-',
    },
    {
      title: t('securityEvent.uri'),
      key: 'uri',
      minWidth: 240,
      ellipsis: { tooltip: true },
      render: (row: SecurityEventItem) => row.uri || '-',
    },
    {
      title: t('securityEvent.httpStatus'),
      key: 'httpStatus',
      width: 90,
      render: (row: SecurityEventItem) => row.httpStatus ?? '-',
    },
    {
      title: t('securityEvent.traceId'),
      key: 'traceId',
      width: 140,
      ellipsis: { tooltip: true },
      render: (row: SecurityEventItem) => row.traceId || '-',
    },
    {
      key: 'operation',
      title: t('common.operation'),
      width: 80,
      fixed: 'right',
      render: (row: SecurityEventItem) =>
        h(
          NButton,
          { text: true, type: 'primary', onClick: () => openDetail(row) },
          { default: () => t('securityEvent.view') }
        ),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<SecurityEventItem>(securityEventList, {
    tableKey: TableKeyEnum.SYSTEM_SECURITY_EVENT_TABLE,
    columns,
    showSetting: true,
    containerClass: '.crm-security-event-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  function search() {
    setLoadListParams(buildFilterParams());
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function reset() {
    queryEventType.value = '';
    ip.value = '';
    time.value = null;
    search();
  }

  async function handleExport() {
    const res = await securityEventExport(buildFilterParams());
    downloadByteFile(res, 'security-events.csv');
  }

  onMounted(() => {
    search();
  });
</script>

<style lang="less" scoped>
  .security-event-page {
    @apply flex h-full flex-col overflow-hidden;
  }
</style>
