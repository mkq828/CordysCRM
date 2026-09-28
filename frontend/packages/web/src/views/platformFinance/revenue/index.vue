<template>
  <div class="pf-revenue-page flex h-full flex-col gap-[16px] overflow-y-auto p-[16px]">
    <!-- 维度切换 -->
    <div class="flex items-center gap-[8px]">
      <span class="text-[14px] text-[#4b5563]">{{ t('platformRevenue.groupBy') }}</span>
      <n-radio-group v-model:value="groupBy" @update:value="load">
        <n-radio-button v-for="opt in groupByOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
      </n-radio-group>
      <div class="flex-1" />
      <n-button quaternary size="small" @click="load">{{ t('platformRevenue.refresh') }}</n-button>
    </div>

    <n-spin :show="loading">
      <!-- KPI 卡片 -->
      <div class="grid grid-cols-4 gap-[16px]">
        <div v-for="kpi in kpis" :key="kpi.key" class="pf-revenue-kpi">
          <div class="pf-revenue-kpi-label">{{ t(`platformRevenue.${kpi.key}`) }}</div>
          <div
            class="pf-revenue-kpi-value"
            :class="{ 'pf-revenue-kpi-value--outstanding': kpi.key === 'outstandingAmount' }"
          >
            {{ kpi.value }}
          </div>
        </div>
      </div>

      <!-- 趋势图 -->
      <CrmCard hide-footer>
        <template #title>{{ t('platformRevenue.trend') }}</template>
        <div ref="chartRef" class="pf-revenue-chart"></div>
      </CrmCard>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NRadioButton, NRadioGroup, NSpin } from 'naive-ui';
  import type { EChartsOption } from 'echarts';
  import { LineChart } from 'echarts/charts';
  import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
  import * as echarts from 'echarts/core';
  import { CanvasRenderer } from 'echarts/renderers';

  import { PlatformRevenueGroupByEnum } from '@lib/shared/enums/platformFinanceEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { PlatformRevenueOverview } from '@lib/shared/models/system/platformFinance';

  import CrmCard from '@/components/pure/crm-card/index.vue';

  import { platformRevenueOverview } from '@/api/modules';

  echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

  const { t } = useI18n();

  const loading = ref(false);
  const data = ref<PlatformRevenueOverview | null>(null);
  const groupBy = ref<PlatformRevenueGroupByEnum>(PlatformRevenueGroupByEnum.MONTH);
  const chartRef = ref<HTMLDivElement | null>(null);

  let chart: echarts.ECharts | null = null;

  const groupByOptions = [
    { label: t('platformRevenue.groupBy.WEEK'), value: PlatformRevenueGroupByEnum.WEEK },
    { label: t('platformRevenue.groupBy.MONTH'), value: PlatformRevenueGroupByEnum.MONTH },
    { label: t('platformRevenue.groupBy.YEAR'), value: PlatformRevenueGroupByEnum.YEAR },
  ];

  function fmtMoney(value?: number | string) {
    return Number(value ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  const kpis = computed(() => {
    const d = data.value;
    return [
      { key: 'contractAmount', value: fmtMoney(d?.contractAmount) },
      { key: 'receivedAmount', value: fmtMoney(d?.receivedAmount) },
      { key: 'outstandingAmount', value: fmtMoney(d?.outstandingAmount) },
      { key: 'invoiceAmount', value: fmtMoney(d?.invoiceAmount) },
    ];
  });

  function chartOption(): EChartsOption {
    const series = data.value?.series || [];
    const buckets = series.map((p) => p.bucket);
    const contracts = series.map((p) => Number(p.contractAmount ?? 0));
    const received = series.map((p) => Number(p.receivedAmount ?? 0));
    const invoiced = series.map((p) => Number(p.invoiceAmount ?? 0));

    return {
      color: ['#4282FF', '#37E0CC', '#FFB958'],
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
          name: t('platformRevenue.contractAmount'),
          type: 'line',
          smooth: true,
          data: contracts,
          symbolSize: 6,
        },
        {
          name: t('platformRevenue.receivedAmount'),
          type: 'line',
          smooth: true,
          data: received,
          symbolSize: 6,
        },
        {
          name: t('platformRevenue.invoiceAmount'),
          type: 'line',
          smooth: true,
          data: invoiced,
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

  async function load() {
    loading.value = true;
    try {
      data.value = await platformRevenueOverview({ groupBy: groupBy.value });
      await nextTick();
      renderChart();
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    window.addEventListener('resize', onResize);
    load();
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', onResize);
    chart?.dispose();
    chart = null;
  });
</script>

<style lang="less" scoped>
  .pf-revenue-page {
    background: #f5f7fa;
  }
  .pf-revenue-kpi {
    padding: 18px 20px;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    background: #ffffff;
    .pf-revenue-kpi-label {
      margin-bottom: 8px;
      font-size: 13px;
      color: #8a94a6;
    }
    .pf-revenue-kpi-value {
      font-size: 24px;
      font-weight: 700;
      color: #1f2329;
      font-variant-numeric: tabular-nums;
      &--outstanding {
        color: #f5222d;
      }
    }
  }
  .pf-revenue-chart {
    width: 100%;
    height: 380px;
  }
</style>
