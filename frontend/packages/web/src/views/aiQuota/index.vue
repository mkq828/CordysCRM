<template>
  <div class="ai-quota-page flex h-full flex-col gap-[16px] overflow-y-auto p-[16px]">
    <!-- 本月概览 -->
    <div class="grid grid-cols-4 gap-[16px]">
      <div v-for="card in overviewCards" :key="card.key" class="ai-quota-kpi">
        <div class="ai-quota-kpi-label">{{ card.label }}</div>
        <div class="ai-quota-kpi-value" :class="{ 'is-red': card.red }">{{ card.value }}</div>
      </div>
    </div>

    <CrmCard hide-footer>
      <template #title>{{ t('aiQuota.usageProgress') }}</template>
      <div class="flex items-center gap-[16px] py-[4px]">
        <n-progress
          class="!flex-1"
          type="line"
          :percentage="usagePercent"
          :status="usagePercent >= 80 ? 'warning' : 'success'"
          :indicator-placement="'inside'"
          :height="18"
        />
        <n-tag :type="statusTagType" size="small">{{ statusLabel }}</n-tag>
      </div>
      <div class="mt-[10px] flex items-center justify-between text-[12px] text-[#8a94a6]">
        <span>{{ t('aiQuota.usedCalls') }}：{{ fmtCalls(overview?.usedCalls) }} / {{ fmtCalls(overview?.quota) }}</span>
        <span>{{ t('aiQuota.softLimitTip', { percent: overview?.softLimitPercent ?? 110 }) }}</span>
      </div>
    </CrmCard>

    <!-- 趋势 -->
    <CrmCard hide-footer>
      <template #title>{{ t('aiQuota.trend') }}</template>
      <div class="mb-[8px] flex items-center gap-[8px]">
        <span class="text-[14px] text-[#4b5563]">{{ t('aiQuota.groupBy') }}</span>
        <n-radio-group v-model:value="groupBy" size="small" @update:value="loadTrend">
          <n-radio-button v-for="opt in groupByOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
        </n-radio-group>
      </div>
      <div ref="trendChartRef" class="ai-quota-chart"></div>
    </CrmCard>

    <!-- 分功能用量 -->
    <CrmCard hide-footer>
      <template #title>{{ t('aiQuota.featureUsage') }}</template>
      <n-spin :show="loading">
        <n-empty v-if="!loading && featureUsage.length === 0" :description="t('aiQuota.empty')" />
        <div v-else class="feature-list">
          <div class="feature-row feature-row-head">
            <span>{{ t('aiQuota.featureName') }}</span>
            <span>{{ t('aiQuota.usedCalls') }}</span>
          </div>
          <div v-for="item in featureUsage" :key="item.featureCode" class="feature-row">
            <span class="truncate">{{ item.featureName || item.featureCode }}</span>
            <span>{{ fmtCalls(item.usedCalls) }}</span>
          </div>
        </div>
      </n-spin>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { NEmpty, NProgress, NRadioButton, NRadioGroup, NSpin, NTag } from 'naive-ui';
  import type { EChartsOption } from 'echarts';
  import { LineChart } from 'echarts/charts';
  import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
  import * as echarts from 'echarts/core';
  import { CanvasRenderer } from 'echarts/renderers';

  import type { AiFeatureUsagePoint, AiQuotaTrendPoint, TenantQuotaOverview } from '@lib/shared/api/modules/aiQuota';
  import { AiQuotaGroupByEnum, AiQuotaStatusEnum } from '@lib/shared/enums/aiQuotaEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';

  import { aiQuotaFeatureUsage, aiQuotaOverview, aiQuotaTrend } from '@/api/modules';

  echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

  const { t } = useI18n();

  const loading = ref(false);
  const overview = ref<TenantQuotaOverview | null>(null);
  const featureUsage = ref<AiFeatureUsagePoint[]>([]);
  const groupBy = ref<AiQuotaGroupByEnum>(AiQuotaGroupByEnum.DAY);
  const trendChartRef = ref<HTMLDivElement | null>(null);

  let trendChart: echarts.ECharts | null = null;

  const groupByOptions = [
    { label: t('aiQuota.groupBy.DAY'), value: AiQuotaGroupByEnum.DAY },
    { label: t('aiQuota.groupBy.MONTH'), value: AiQuotaGroupByEnum.MONTH },
  ];

  function fmtCalls(value?: number | string) {
    const n = Number(value ?? 0);
    return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 });
  }

  const usagePercent = computed(() => {
    const used = Number(overview.value?.usedPercent ?? 0);
    return Math.min(Math.max(used, 0), 100);
  });

  const statusTagType = computed(() => {
    switch (overview.value?.status) {
      case AiQuotaStatusEnum.SOFT_LIMITED:
        return 'warning';
      case AiQuotaStatusEnum.HARD_LIMITED:
      case AiQuotaStatusEnum.CIRCUIT_BROKEN:
        return 'error';
      case AiQuotaStatusEnum.RATE_LIMITED:
        return 'info';
      default:
        return 'success';
    }
  });

  const statusLabel = computed(() => {
    const key = overview.value?.status || AiQuotaStatusEnum.NORMAL;
    return t(`aiQuota.status.${key}`);
  });

  const overviewCards = computed(() => [
    { key: 'monthlyUsed', label: t('aiQuota.monthlyUsed'), value: fmtCalls(overview.value?.usedCalls), red: false },
    { key: 'monthlyQuota', label: t('aiQuota.monthlyQuota'), value: fmtCalls(overview.value?.quota), red: false },
    {
      key: 'remaining',
      label: t('aiQuota.remaining'),
      value: fmtCalls(overview.value?.remainingCalls),
      red: Number(overview.value?.remainingCalls ?? 0) <= 0,
    },
    { key: 'todayUsed', label: t('aiQuota.todayUsed'), value: fmtCalls(overview.value?.todayCalls), red: false },
  ]);

  function trendChartOption(points: AiQuotaTrendPoint[]): EChartsOption {
    const buckets = points.map((p) => p.bucket);
    const values = points.map((p) => Number(p.usedCalls ?? 0));

    return {
      color: ['#4282FF'],
      tooltip: { trigger: 'axis' },
      grid: { top: 24, left: 16, right: 16, bottom: 8, containLabel: true },
      xAxis: {
        type: 'category',
        data: buckets,
        boundaryGap: false,
        axisLabel: { rotate: buckets.length > 12 ? 30 : 0, interval: 0 },
        axisLine: { lineStyle: { color: 'rgba(0,0,0,0.2)' } },
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { type: 'dashed' } },
      },
      series: [
        {
          name: t('aiQuota.usedCalls'),
          type: 'line',
          smooth: true,
          data: values,
          areaStyle: { opacity: 0.08 },
          symbolSize: 6,
        },
      ],
    };
  }

  function renderTrendChart(points: AiQuotaTrendPoint[]) {
    if (!trendChartRef.value) return;
    trendChart = trendChart ?? echarts.init(trendChartRef.value);
    trendChart.setOption(trendChartOption(points), true);
  }

  function onResize() {
    trendChart?.resize();
  }

  async function loadOverview() {
    try {
      overview.value = await aiQuotaOverview();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function loadFeatureUsage() {
    try {
      featureUsage.value = await aiQuotaFeatureUsage();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function loadTrend() {
    loading.value = true;
    try {
      const points = await aiQuotaTrend(groupBy.value);
      await nextTick();
      renderTrendChart(points);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    window.addEventListener('resize', onResize);
    loadOverview();
    loadFeatureUsage();
    loadTrend();
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', onResize);
    trendChart?.dispose();
    trendChart = null;
  });
</script>

<style lang="less" scoped>
  .ai-quota-page {
    background: #f5f7fa;
  }
  .ai-quota-kpi {
    padding: 18px 20px;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    background: #ffffff;
    .ai-quota-kpi-label {
      margin-bottom: 8px;
      font-size: 13px;
      color: #8a94a6;
    }
    .ai-quota-kpi-value {
      font-size: 24px;
      font-weight: 700;
      color: #1f2329;
      font-variant-numeric: tabular-nums;
      &.is-red {
        color: #f5222d;
      }
    }
  }
  .ai-quota-chart {
    width: 100%;
    height: 300px;
  }
  .feature-list {
    display: flex;
    flex-direction: column;
    .feature-row {
      display: grid;
      align-items: center;
      padding: 10px 4px;
      font-size: 13px;
      border-bottom: 1px solid #f0f2f5;
      color: #1f2329;
      grid-template-columns: 1fr 160px;
      gap: 8px;
      &:last-child {
        border: none;
      }
      &.feature-row-head {
        color: #8a94a6;
      }
      span:last-child {
        text-align: right;
        font-variant-numeric: tabular-nums;
      }
    }
  }
</style>
