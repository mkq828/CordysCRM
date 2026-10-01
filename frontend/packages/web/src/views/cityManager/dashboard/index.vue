<template>
  <div class="cm-dashboard-page flex h-full flex-col gap-[16px] overflow-y-auto p-[16px]">
    <!-- 维度切换 -->
    <div class="flex items-center gap-[8px]">
      <span class="text-[14px] text-[#4b5563]">{{ t('cityManagerPerformance.groupBy') }}</span>
      <n-radio-group v-model:value="groupBy" @update:value="load">
        <n-radio-button v-for="opt in groupByOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
      </n-radio-group>
      <div class="flex-1" />
      <!-- 城市合伙人本人只能看自己，admin 才显示经理下拉 -->
      <n-select
        v-if="!isCityManager"
        v-model:value="managerId"
        :options="managerOptions"
        :placeholder="t('cityManagerPerformance.manager')"
        clearable
        class="w-[220px]"
        @update:value="load"
      />
      <n-button quaternary size="small" @click="load">{{ t('cityManagerPerformance.refresh') }}</n-button>
    </div>

    <n-spin :show="loading">
      <!-- 业绩汇总表 -->
      <CrmCard hide-footer class="mb-[16px]">
        <template #title>{{ t('cityManagerPerformance.summary') }}</template>
        <n-data-table
          :columns="summaryColumns"
          :data="summaryData"
          :bordered="false"
          size="small"
          :row-key="(row) => row.managerId"
        />
      </CrmCard>

      <!-- 趋势图 -->
      <CrmCard hide-footer>
        <template #title>{{ t('cityManagerPerformance.trend') }}</template>
        <div ref="chartRef" class="cm-dashboard-chart"></div>
      </CrmCard>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
  import { NButton, NDataTable, NRadioButton, NRadioGroup, NSelect, NSpin } from 'naive-ui';
  import type { EChartsOption } from 'echarts';
  import { LineChart } from 'echarts/charts';
  import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
  import * as echarts from 'echarts/core';
  import { CanvasRenderer } from 'echarts/renderers';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CityManagerItem, CityManagerPerformanceSummary } from '@lib/shared/models/system/cityManager';

  import CrmCard from '@/components/pure/crm-card/index.vue';

  import { cityManagerPageList, cityManagerPerformanceOverview } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  import type { DataTableColumns } from 'naive-ui';

  echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

  const { t } = useI18n();
  const userStore = useUserStore();
  const isCityManager = computed(() => userStore.isCityManager);

  const loading = ref(false);
  const summary = ref<CityManagerPerformanceSummary[]>([]);
  const seriesData = ref<{ bucket: string; signedCount: number; contractAmount: number; paymentAmount: number }[]>([]);
  const groupBy = ref<'WEEK' | 'MONTH' | 'YEAR'>('MONTH');
  const managerId = ref<string | null>(null);
  const managerOptions = ref<{ label: string; value: string }[]>([]);
  const chartRef = ref<HTMLDivElement | null>(null);

  let chart: echarts.ECharts | null = null;

  const groupByOptions = [
    { label: t('cityManagerPerformance.groupBy.WEEK'), value: 'WEEK' },
    { label: t('cityManagerPerformance.groupBy.MONTH'), value: 'MONTH' },
    { label: t('cityManagerPerformance.groupBy.YEAR'), value: 'YEAR' },
  ];

  function fmtMoney(value?: number | string) {
    return Number(value ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  const summaryColumns = computed<DataTableColumns<CityManagerPerformanceSummary>>(() => [
    { title: t('cityManagerPerformance.name'), key: 'name', width: 160 },
    { title: t('cityManagerPerformance.signedCount'), key: 'signedCount', width: 120, align: 'left' },
    {
      title: t('cityManagerPerformance.contractAmount'),
      key: 'contractAmount',
      width: 160,
      align: 'left',
      render: (row) => fmtMoney(row.contractAmount),
    },
    {
      title: t('cityManagerPerformance.paymentAmount'),
      key: 'paymentAmount',
      width: 160,
      align: 'left',
      render: (row) => fmtMoney(row.paymentAmount),
    },
  ]);

  const summaryData = computed(() => summary.value);

  function chartOption(): EChartsOption {
    const series = seriesData.value;
    const buckets = series.map((p) => p.bucket);
    const signed = series.map((p) => Number(p.signedCount ?? 0));
    const contract = series.map((p) => Number(p.contractAmount ?? 0));
    const payment = series.map((p) => Number(p.paymentAmount ?? 0));

    return {
      color: ['#4282FF', '#FFB958', '#37E0CC'],
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { top: 0 },
      grid: { top: 40, left: 16, right: 16, bottom: 8, containLabel: true },
      xAxis: {
        type: 'category',
        data: buckets,
        boundaryGap: false,
        axisLabel: { rotate: buckets.length > 12 ? 30 : 0, interval: 0 },
        axisLine: { lineStyle: { color: 'rgba(0,0,0,0.2)' } },
      },
      yAxis: {
        type: 'value',
        axisLabel: { formatter: (v: number) => fmtMoney(v) },
        splitLine: { lineStyle: { type: 'dashed' } },
      },
      series: [
        {
          name: t('cityManagerPerformance.signedCount'),
          type: 'line',
          smooth: true,
          data: signed,
          symbolSize: 6,
        },
        {
          name: t('cityManagerPerformance.contractAmount'),
          type: 'line',
          smooth: true,
          data: contract,
          symbolSize: 6,
        },
        {
          name: t('cityManagerPerformance.paymentAmount'),
          type: 'line',
          smooth: true,
          data: payment,
          symbolSize: 6,
        },
      ],
    };
  }

  function renderChart() {
    if (!chartRef.value) return;
    chart = chart ?? echarts.init(chartRef.value);
    chart.setOption(chartOption(), true);
  }

  function onResize() {
    chart?.resize();
  }

  async function loadManagers() {
    if (isCityManager.value) return;
    try {
      const res = await cityManagerPageList({ current: 1, pageSize: 500 });
      managerOptions.value = (res.list || []).map((m: CityManagerItem) => ({ label: m.name, value: m.id }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function load() {
    loading.value = true;
    try {
      const res = await cityManagerPerformanceOverview({
        groupBy: groupBy.value,
        managerId: isCityManager.value ? undefined : managerId.value || undefined,
      });
      summary.value = res.summary || [];
      seriesData.value = (res.series || []).map((p) => ({
        bucket: p.bucket,
        signedCount: Number(p.signedCount ?? 0),
        contractAmount: Number(p.contractAmount ?? 0),
        paymentAmount: Number(p.paymentAmount ?? 0),
      }));
      await nextTick();
      renderChart();
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    window.addEventListener('resize', onResize);
    loadManagers();
    load();
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', onResize);
    chart?.dispose();
    chart = null;
  });
</script>

<style lang="less" scoped>
  .cm-dashboard-page {
    background: #f5f7fa;
  }
  .cm-dashboard-chart {
    width: 100%;
    height: 380px;
  }
</style>
