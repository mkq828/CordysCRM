<template>
  <div class="platform-dashboard">
    <div class="pd-header">
      <n-button quaternary size="small" class="pd-back" @click="goBack">
        {{ t('dashboard.platform.back') }}
      </n-button>
      <div class="pd-title">{{ t('dashboard.platform.title') }}</div>
      <div class="pd-time">{{ loadedAt }}</div>
    </div>

    <n-spin :show="loading">
      <div class="pd-body">
        <div v-if="data" class="pd-kpis">
          <div v-for="kpi in kpis" :key="kpi.key" class="pd-kpi">
            <div class="pd-kpi-label">{{ t(`dashboard.platform.${kpi.key}`) }}</div>
            <div class="pd-kpi-value">{{ kpi.value }}</div>
          </div>
        </div>

        <div class="pd-charts">
          <div class="pd-card">
            <div ref="pieRef" class="pd-chart"></div>
          </div>
          <div class="pd-card pd-card--wide">
            <div ref="barRef" class="pd-chart"></div>
          </div>
        </div>

        <div class="pd-card pd-card--table">
          <div class="pd-card-title">{{ t('dashboard.platform.tenantDetail') }}</div>
          <div class="pd-table">
            <table>
              <thead>
                <tr>
                  <th v-for="col in columns" :key="col.key" :class="{ 'pd-col--right': col.align === 'right' }">
                    {{ col.label }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in pagedTableRows" :key="row.organizationId">
                  <td v-for="col in columns" :key="col.key" :class="{ 'pd-col--right': col.align === 'right' }">
                    <span v-if="col.key === 'active'" class="pd-active" :class="{ 'pd-active--on': row.active }">
                      {{ row.active ? t('dashboard.platform.active.yes') : t('dashboard.platform.active.no') }}
                    </span>
                    <template v-else>{{ row[col.key] }}</template>
                  </td>
                </tr>
                <tr v-if="!tableRows.length">
                  <td :colspan="columns.length" class="pd-table-empty">{{ t('dashboard.platform.loadError') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="tableRows.length" class="pd-table-pagination">
            <n-pagination v-model:page="currentPage" :page-count="pageCount" />
          </div>
        </div>
      </div>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { NButton, NPagination, NSpin } from 'naive-ui';
  import dayjs from 'dayjs';
  import type { EChartsOption } from 'echarts';
  import { BarChart, PieChart } from 'echarts/charts';
  import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components';
  import * as echarts from 'echarts/core';
  import { CanvasRenderer } from 'echarts/renderers';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { PlatformDashboardResponse } from '@lib/shared/models/system/platformDashboard';

  import { getPlatformDashboard } from '@/api/modules';

  echarts.use([PieChart, BarChart, GridComponent, LegendComponent, TitleComponent, TooltipComponent, CanvasRenderer]);

  const router = useRouter();
  const { t } = useI18n();

  const loading = ref(false);
  const data = ref<PlatformDashboardResponse | null>(null);
  const loadedAt = ref('');
  const pieRef = ref<HTMLDivElement | null>(null);
  const barRef = ref<HTMLDivElement | null>(null);

  let pieChart: echarts.ECharts | null = null;
  let barChart: echarts.ECharts | null = null;

  const palette = [
    '#4282FF',
    '#37E0CC',
    '#FFB958',
    '#FF9562',
    '#7F60F9',
    '#CC71E2',
    '#76DE74',
    '#3BC2FC',
    '#FBF15A',
    '#E94256',
  ];

  function fmtCount(v?: number) {
    return Number(v || 0).toLocaleString('zh-CN');
  }

  function fmtAmount(v?: number | string) {
    return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  const kpis = computed(() => {
    const d = data.value;
    return [
      { key: 'totalTenant', value: fmtCount(d?.totalTenant) },
      { key: 'enterpriseTenant', value: fmtCount(d?.enterpriseTenant) },
      { key: 'personalTenant', value: fmtCount(d?.personalTenant) },
      { key: 'activeTenant', value: fmtCount(d?.activeTenant) },
      { key: 'totalAccount', value: fmtCount(d?.totalAccount) },
      { key: 'totalClue', value: fmtCount(d?.totalClue) },
      { key: 'totalCustomer', value: fmtCount(d?.totalCustomer) },
      { key: 'totalOpportunity', value: fmtCount(d?.totalOpportunity) },
      { key: 'totalOrder', value: fmtCount(d?.totalOrder) },
      { key: 'totalContractAmount', value: fmtAmount(d?.totalContractAmount) },
      { key: 'totalReceivedAmount', value: fmtAmount(d?.totalReceivedAmount) },
      { key: 'totalOutstandingAmount', value: fmtAmount(d?.totalOutstandingAmount) },
    ];
  });

  interface TableRow {
    organizationId: string;
    organizationName: string;
    orgType: string;
    accountCount: string;
    customerCount: string;
    clueCount: string;
    opportunityCount: string;
    orderCount: string;
    contractAmount: string;
    receivedAmount: string;
    outstandingAmount: string;
    usageDays: string;
    activeDays30: string;
    lastLoginCity: string;
    mainLoginCity: string;
    lastLoginTime: string;
    active: boolean;
  }

  interface TableColumn {
    key: keyof TableRow;
    label: string;
    align: 'left' | 'right';
  }

  const columns = computed<TableColumn[]>(() => [
    { key: 'organizationName', label: t('dashboard.platform.tenantName'), align: 'left' },
    { key: 'orgType', label: t('dashboard.platform.orgType'), align: 'left' },
    { key: 'accountCount', label: t('dashboard.platform.accountCount'), align: 'right' },
    { key: 'customerCount', label: t('dashboard.platform.customerCount'), align: 'right' },
    { key: 'clueCount', label: t('dashboard.platform.clueCount'), align: 'right' },
    { key: 'opportunityCount', label: t('dashboard.platform.opportunityCount'), align: 'right' },
    { key: 'orderCount', label: t('dashboard.platform.orderCount'), align: 'right' },
    { key: 'contractAmount', label: t('dashboard.platform.contractAmount'), align: 'right' },
    { key: 'receivedAmount', label: t('dashboard.platform.receivedAmount'), align: 'right' },
    { key: 'outstandingAmount', label: t('dashboard.platform.outstandingAmount'), align: 'right' },
    { key: 'usageDays', label: t('dashboard.platform.usageDays'), align: 'right' },
    { key: 'activeDays30', label: t('dashboard.platform.activeDays30'), align: 'right' },
    { key: 'lastLoginCity', label: t('dashboard.platform.lastLoginCity'), align: 'left' },
    { key: 'mainLoginCity', label: t('dashboard.platform.mainLoginCity'), align: 'left' },
    { key: 'lastLoginTime', label: t('dashboard.platform.lastLoginTime'), align: 'left' },
    { key: 'active', label: t('dashboard.platform.active'), align: 'left' },
  ]);

  const tableRows = computed<TableRow[]>(() => {
    return (data.value?.rows || []).map((row) => ({
      organizationId: row.organizationId,
      organizationName: row.organizationName || '-',
      orgType: t(`dashboard.platform.orgType.${row.orgType}`),
      accountCount: fmtCount(row.accountCount),
      customerCount: fmtCount(row.customerCount),
      clueCount: fmtCount(row.clueCount),
      opportunityCount: fmtCount(row.opportunityCount),
      orderCount: fmtCount(row.orderCount),
      contractAmount: fmtAmount(row.contractAmount),
      receivedAmount: fmtAmount(row.receivedAmount),
      outstandingAmount: fmtAmount(row.outstandingAmount),
      usageDays: String(row.usageDays ?? 0),
      activeDays30: String(row.activeDays30 ?? 0),
      lastLoginCity: row.lastLoginCity || '-',
      mainLoginCity: row.mainLoginCity || '-',
      lastLoginTime: row.lastLoginTime
        ? dayjs(row.lastLoginTime).format('YYYY-MM-DD HH:mm')
        : t('dashboard.platform.neverLogin'),
      active: !!row.active,
    }));
  });

  // 租户明细分页（每页 10 条）
  const tenantPageSize = 10;
  const currentPage = ref(1);
  const pageCount = computed(() => Math.ceil(tableRows.value.length / tenantPageSize));
  const pagedTableRows = computed(() => {
    const start = (currentPage.value - 1) * tenantPageSize;
    return tableRows.value.slice(start, start + tenantPageSize);
  });

  function pieOption(): EChartsOption {
    return {
      color: palette,
      title: {
        text: t('dashboard.platform.tenantTypeDist'),
        left: 'center',
        top: 8,
        textStyle: { color: '#e6ecf5', fontSize: 16, fontWeight: 600 },
      },
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: {
        orient: 'vertical',
        right: '6%',
        top: 'center',
        textStyle: { color: '#c7d1e0' },
      },
      series: [
        {
          type: 'pie',
          radius: ['42%', '66%'],
          center: ['36%', '55%'],
          data: [
            { name: t('dashboard.platform.orgType.ENTERPRISE'), value: data.value?.enterpriseTenant || 0 },
            { name: t('dashboard.platform.orgType.PERSONAL'), value: data.value?.personalTenant || 0 },
          ],
          label: { color: '#c7d1e0', formatter: '{b}\n{c}' },
          labelLine: { lineStyle: { color: '#c7d1e0' } },
          itemStyle: { borderColor: '#0b1a33', borderWidth: 2 },
        },
      ],
    };
  }

  function barOption(): EChartsOption {
    const top = [...(data.value?.rows || [])]
      .sort((a, b) => (b.customerCount || 0) - (a.customerCount || 0))
      .slice(0, 10);
    const names = top.map((row) => row.organizationName || '-');
    const customers = top.map((row) => row.customerCount || 0);
    const orders = top.map((row) => row.orderCount || 0);

    return {
      color: palette,
      title: {
        text: t('dashboard.platform.tenantCompare'),
        left: 'center',
        top: 8,
        textStyle: { color: '#e6ecf5', fontSize: 16, fontWeight: 600 },
      },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { top: 40, textStyle: { color: '#c7d1e0' } },
      grid: { top: 80, left: 8, right: 8, bottom: 8, containLabel: true },
      xAxis: {
        type: 'category',
        data: names,
        axisLabel: { color: '#c7d1e0', rotate: 30, interval: 0 },
        axisLine: { lineStyle: { color: 'rgba(199,209,224,0.25)' } },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { color: '#c7d1e0' },
        splitLine: { lineStyle: { color: 'rgba(199,209,224,0.15)', type: 'dashed' } },
      },
      series: [
        {
          name: t('dashboard.platform.customerCount'),
          type: 'bar',
          data: customers,
          barMaxWidth: 24,
          itemStyle: { borderRadius: [2, 2, 0, 0] },
        },
        {
          name: t('dashboard.platform.orderCount'),
          type: 'bar',
          data: orders,
          barMaxWidth: 24,
          itemStyle: { borderRadius: [2, 2, 0, 0] },
        },
      ],
    };
  }

  function renderCharts() {
    if (pieRef.value) {
      pieChart = pieChart ?? echarts.init(pieRef.value);
      pieChart.setOption(pieOption(), true);
    }
    if (barRef.value) {
      barChart = barChart ?? echarts.init(barRef.value);
      barChart.setOption(barOption(), true);
    }
  }

  function onResize() {
    pieChart?.resize();
    barChart?.resize();
  }

  async function load() {
    loading.value = true;
    try {
      data.value = await getPlatformDashboard();
      currentPage.value = 1;
      loadedAt.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
      await nextTick();
      renderCharts();
    } finally {
      loading.value = false;
    }
  }

  function goBack() {
    router.back();
  }

  onMounted(() => {
    window.addEventListener('resize', onResize);
    load();
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', onResize);
    pieChart?.dispose();
    barChart?.dispose();
    pieChart = null;
    barChart = null;
  });
</script>

<style lang="less" scoped>
  .platform-dashboard {
    overflow-x: hidden;
    overflow-y: auto;
    padding: 20px 24px;
    height: calc(100vh - 96px);
    border-radius: 12px;
    color: #e6ecf5;
    background: linear-gradient(160deg, #0b1a33 0%, #10233f 55%, #0d1b33 100%);
    .pd-header {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 20px;
      .pd-back {
        color: #c7d1e0;
      }
      .pd-title {
        flex: 1;
        font-size: 24px;
        font-weight: 600;
        letter-spacing: 1px;
      }
      .pd-time {
        font-size: 13px;
        color: #8ea0bd;
      }
    }
    .pd-body {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
    .pd-kpis {
      display: grid;
      grid-template-columns: repeat(6, 1fr);
      gap: 16px;
    }
    .pd-kpi {
      padding: 18px 20px;
      border: 1px solid rgb(66 130 255 / 20%);
      border-radius: 10px;
      background: rgb(66 130 255 / 10%);
      .pd-kpi-label {
        margin-bottom: 8px;
        font-size: 13px;
        color: #8ea0bd;
      }
      .pd-kpi-value {
        font-size: 24px;
        font-weight: 700;
        color: #e6ecf5;
        font-variant-numeric: tabular-nums;
      }
    }
    .pd-charts {
      display: grid;
      grid-template-columns: 1fr 2fr;
      gap: 20px;
    }
    .pd-card {
      padding: 16px;
      border: 1px solid rgb(142 160 189 / 15%);
      border-radius: 10px;
      background: rgb(16 35 63 / 60%);
      &--table {
        overflow: hidden;
        padding: 0;
      }
    }
    .pd-chart {
      width: 100%;
      height: 320px;
    }
    .pd-card-title {
      padding: 16px 20px;
      font-size: 16px;
      font-weight: 600;
      border-bottom: 1px solid rgb(142 160 189 / 15%);
      color: #e6ecf5;
    }
    .pd-table {
      overflow-x: auto;
      table {
        width: 100%;
        min-width: 1200px;
        border-collapse: collapse;
        font-size: 13px;
        th,
        td {
          padding: 12px 16px;
          border-bottom: 1px solid rgb(142 160 189 / 12%);
          text-align: left;
          white-space: nowrap;
        }
        th {
          font-weight: 500;
          color: #8ea0bd;
          background: rgb(66 130 255 / 6%);
        }
        td {
          color: #d6e0ef;
          font-variant-numeric: tabular-nums;
        }
        .pd-col--right {
          text-align: right;
        }
        .pd-table-empty {
          padding: 40px 16px;
          text-align: center;
          color: #8ea0bd;
        }
      }
    }
    .pd-table-pagination {
      display: flex;
      justify-content: flex-end;
      padding: 12px 20px;
      border-top: 1px solid rgb(142 160 189 / 15%);
    }
    .pd-active {
      color: #8ea0bd;
      &--on {
        color: #37e0cc;
      }
    }
  }
</style>
