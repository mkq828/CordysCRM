<template>
  <div class="paq-page flex h-full flex-col overflow-y-auto p-[16px]">
    <n-tabs v-model:value="activeTab" type="line" class="paq-tabs">
      <!-- 成本看板 -->
      <n-tab-pane name="cost" :tab="t('platformAiQuota.costTab')">
        <div class="flex flex-col gap-[16px]">
          <div class="flex items-center gap-[8px]">
            <span class="text-[14px] text-[#4b5563]">{{ t('platformAiQuota.groupBy') }}</span>
            <n-radio-group v-model:value="groupBy" @update:value="loadCost">
              <n-radio-button v-for="opt in groupByOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
            </n-radio-group>
            <div class="flex-1" />
            <n-button quaternary size="small" @click="loadCost">{{ t('platformAiQuota.refresh') }}</n-button>
          </div>

          <n-spin :show="costLoading">
            <div class="grid grid-cols-3 gap-[16px]">
              <div v-for="kpi in costKpis" :key="kpi.key" class="paq-kpi">
                <div class="paq-kpi-label">{{ kpi.label }}</div>
                <div class="paq-kpi-value">{{ kpi.value }}</div>
              </div>
            </div>
            <div class="mt-[4px] text-[12px] text-[#8a94a6]">{{ t('platformAiQuota.costEstimated') }}</div>

            <CrmCard hide-footer class="!mt-[16px]">
              <template #title>{{ t('platformAiQuota.costTrend') }}</template>
              <div ref="costChartRef" class="paq-chart"></div>
            </CrmCard>

            <CrmCard hide-footer class="!mt-[16px]">
              <template #title>{{ t('platformAiQuota.tenantCost') }}</template>
              <n-data-table
                :columns="tenantColumns"
                :data="tenantRows"
                :bordered="false"
                :scroll-x="800"
                size="small"
              />
            </CrmCard>
          </n-spin>
        </div>
      </n-tab-pane>

      <!-- 模型单价 -->
      <n-tab-pane name="price" :tab="t('platformAiQuota.priceTab')">
        <div class="flex flex-col gap-[12px]">
          <div class="flex justify-end">
            <n-button type="primary" size="small" @click="openPriceEdit()">
              {{ t('platformAiQuota.addModel') }}
            </n-button>
          </div>
          <n-data-table
            :columns="priceColumns"
            :data="modelPrices"
            :bordered="false"
            :loading="priceLoading"
            size="small"
          />
        </div>
      </n-tab-pane>

      <!-- 全局配置 -->
      <n-tab-pane name="config" :tab="t('platformAiQuota.configTab')">
        <CrmCard hide-footer class="!max-w-[720px]">
          <div class="flex flex-col gap-[16px]">
            <div class="config-row">
              <span class="config-label">{{ t('platformAiQuota.tokensPerCall') }}</span>
              <n-input-number v-model:value="config.tokensPerCall" :min="1" class="!w-[200px]" />
            </div>
            <div class="config-row">
              <span class="config-label">{{ t('platformAiQuota.softLimitPercent') }}</span>
              <n-input-number v-model:value="config.softLimitPercent" :min="100" :max="200" class="!w-[200px]" />
            </div>
            <div class="config-row">
              <span class="config-label">{{ t('platformAiQuota.trialQuota') }}</span>
              <n-input-number v-model:value="config.trialQuota" :min="0" class="!w-[200px]" />
            </div>
            <div class="config-row">
              <span class="config-label">{{ t('platformAiQuota.minuteCallLimit') }}</span>
              <n-input-number v-model:value="config.minuteCallLimit" :min="1" class="!w-[200px]" />
            </div>
            <div class="config-row">
              <span class="config-label">{{ t('platformAiQuota.dailyCallLimit') }}</span>
              <n-input-number v-model:value="config.dailyCallLimit" :min="1" class="!w-[200px]" />
            </div>
            <div class="config-row">
              <span class="config-label">{{ t('platformAiQuota.dailyCostThreshold') }}</span>
              <n-input-number v-model:value="config.dailyCostThreshold" :min="0" :step="10" class="!w-[200px]" />
            </div>
            <div class="flex justify-end gap-[8px]">
              <n-button @click="loadConfig">{{ t('platformAiQuota.reset') }}</n-button>
              <n-button type="primary" :loading="configSaving" @click="saveConfig">
                {{ t('platformAiQuota.save') }}
              </n-button>
            </div>
          </div>
        </CrmCard>
      </n-tab-pane>

      <!-- 模拟记账（验证用） -->
      <n-tab-pane name="mock" :tab="t('platformAiQuota.mockTab')">
        <CrmCard hide-footer class="!max-w-[720px]">
          <template #title>{{ t('platformAiQuota.mockTitle') }}</template>
          <div class="mb-[12px] text-[12px] text-[#8a94a6]">{{ t('platformAiQuota.mockHint') }}</div>
          <div class="flex flex-col gap-[12px]">
            <div class="config-row">
              <span class="config-label">{{ t('platformAiQuota.mockOrg') }}</span>
              <n-input
                v-model:value="mock.organizationId"
                class="!w-[260px]"
                :placeholder="t('platformAiQuota.mockOrgPh')"
              />
            </div>
            <div class="config-row">
              <span class="config-label">{{ t('platformAiQuota.mockFeature') }}</span>
              <n-select
                v-model:value="mock.featureCode"
                class="!w-[260px]"
                :options="featureCodeOptions"
                :placeholder="t('platformAiQuota.mockFeaturePh')"
              />
            </div>
            <div class="config-row">
              <span class="config-label">{{ t('platformAiQuota.mockModel') }}</span>
              <n-input
                v-model:value="mock.modelCode"
                class="!w-[260px]"
                :placeholder="t('platformAiQuota.mockModelPh')"
              />
            </div>
            <div class="config-row">
              <span class="config-label">{{ t('platformAiQuota.mockInput') }}</span>
              <n-input-number v-model:value="mock.inputTokens" :min="0" class="!w-[260px]" />
            </div>
            <div class="config-row">
              <span class="config-label">{{ t('platformAiQuota.mockOutput') }}</span>
              <n-input-number v-model:value="mock.outputTokens" :min="0" class="!w-[260px]" />
            </div>
            <div class="flex items-center justify-end gap-[8px]">
              <n-button type="primary" :loading="mock.loading" @click="submitMock">
                {{ t('platformAiQuota.mockSubmit') }}
              </n-button>
            </div>
            <n-alert v-if="mock.result" type="info" :bordered="false">
              <div>{{ t('platformAiQuota.mockStatus') }}：{{ mock.result.status }}</div>
              <div>{{ t('platformAiQuota.mockCostCalls') }}：{{ fmtCalls(mock.result.costCalls) }}</div>
              <div>{{ t('platformAiQuota.mockUsedCalls') }}：{{ fmtCalls(mock.result.usedCalls) }}</div>
              <div>{{ t('platformAiQuota.mockQuota') }}：{{ fmtCalls(mock.result.quota) }}</div>
            </n-alert>
          </div>
        </CrmCard>
      </n-tab-pane>
    </n-tabs>

    <!-- 模型单价编辑弹窗 -->
    <n-modal v-model:show="priceModal.show" preset="card" :title="priceModal.title" class="!w-[480px]">
      <div class="flex flex-col gap-[12px]">
        <div class="config-row">
          <span class="config-label">{{ t('platformAiQuota.modelCode') }}</span>
          <n-input v-model:value="priceModal.form.modelCode" :disabled="!!priceModal.form.id" class="!w-[260px]" />
        </div>
        <div class="config-row">
          <span class="config-label">{{ t('platformAiQuota.modelName') }}</span>
          <n-input v-model:value="priceModal.form.modelName" class="!w-[260px]" />
        </div>
        <div class="config-row">
          <span class="config-label">{{ t('platformAiQuota.inputPrice') }}</span>
          <n-input-number v-model:value="priceModal.form.inputPrice" :min="0" :step="0.0001" class="!w-[260px]" />
        </div>
        <div class="config-row">
          <span class="config-label">{{ t('platformAiQuota.outputPrice') }}</span>
          <n-input-number v-model:value="priceModal.form.outputPrice" :min="0" :step="0.0001" class="!w-[260px]" />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-[8px]">
          <n-button @click="priceModal.show = false">{{ t('platformAiQuota.cancel') }}</n-button>
          <n-button type="primary" :loading="priceModal.loading" @click="submitPrice">
            {{ t('platformAiQuota.save') }}
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import {
    NAlert,
    NButton,
    NDataTable,
    NInput,
    NInputNumber,
    NModal,
    NRadioButton,
    NRadioGroup,
    NSelect,
    NSpin,
    NTabPane,
    NTabs,
    useMessage,
  } from 'naive-ui';
  import type { EChartsOption } from 'echarts';
  import { LineChart } from 'echarts/charts';
  import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
  import * as echarts from 'echarts/core';
  import { CanvasRenderer } from 'echarts/renderers';

  import { AiQuotaGroupByEnum } from '@lib/shared/enums/aiQuotaEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AdminAiCostOverview,
    AdminAiCostTenantRow,
    AiModelPrice,
    AiQuotaConfig,
    AiQuotaRecordResult,
  } from '@lib/shared/models/system/platformAiQuota';

  import CrmCard from '@/components/pure/crm-card/index.vue';

  import {
    platformAiQuotaCost,
    platformAiQuotaGetConfig,
    platformAiQuotaMockRecord,
    platformAiQuotaModelPriceList,
    platformAiQuotaModelPriceSave,
    platformAiQuotaUpdateConfig,
  } from '@/api/modules';

  import type { DataTableColumns } from 'naive-ui';

  echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

  const { t } = useI18n();
  const Message = useMessage();

  const activeTab = ref('cost');

  const groupBy = ref<AiQuotaGroupByEnum>(AiQuotaGroupByEnum.DAY);
  const groupByOptions = [
    { label: t('platformAiQuota.groupBy.DAY'), value: AiQuotaGroupByEnum.DAY },
    { label: t('platformAiQuota.groupBy.MONTH'), value: AiQuotaGroupByEnum.MONTH },
  ];

  function fmtMoney(value?: number | string) {
    const n = Number(value ?? 0);
    return n.toLocaleString('zh-CN', { minimumFractionDigits: 4, maximumFractionDigits: 4 });
  }

  function fmtCalls(value?: number | string) {
    return Number(value ?? 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 });
  }

  function fmtTokens(value?: number | string) {
    return Number(value ?? 0).toLocaleString('zh-CN');
  }

  // ==================== 成本看板 ====================
  const costLoading = ref(false);
  const costData = ref<AdminAiCostOverview | null>(null);
  const costChartRef = ref<HTMLDivElement | null>(null);
  let costChart: echarts.ECharts | null = null;

  const costKpis = computed(() => {
    const d = costData.value;
    return [
      { key: 'totalCost', label: t('platformAiQuota.totalCost'), value: `¥ ${fmtMoney(d?.totalCost)}` },
      { key: 'totalTokens', label: t('platformAiQuota.totalTokens'), value: fmtTokens(d?.totalTokens) },
      { key: 'totalCalls', label: t('platformAiQuota.totalCalls'), value: fmtCalls(d?.totalCalls) },
    ];
  });

  const tenantRows = computed<AdminAiCostTenantRow[]>(() => costData.value?.tenantRows || []);

  const tenantColumns = computed<DataTableColumns<AdminAiCostTenantRow>>(() => [
    { title: t('platformAiQuota.orgName'), key: 'organizationName' },
    { title: t('platformAiQuota.costCalls'), key: 'costCalls', render: (row) => fmtCalls(row.costCalls) },
    { title: t('platformAiQuota.inputTokens'), key: 'inputTokens', render: (row) => fmtTokens(row.inputTokens) },
    { title: t('platformAiQuota.outputTokens'), key: 'outputTokens', render: (row) => fmtTokens(row.outputTokens) },
    { title: t('platformAiQuota.cost'), key: 'cost', render: (row) => fmtMoney(row.cost) },
  ]);

  function costChartOption(): EChartsOption {
    const series = costData.value?.series || [];
    const buckets = series.map((p) => p.bucket);
    const values = series.map((p) => Number(p.cost ?? 0));

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
      yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
      series: [
        {
          name: t('platformAiQuota.cost'),
          type: 'line',
          smooth: true,
          data: values,
          areaStyle: { opacity: 0.08 },
          symbolSize: 6,
        },
      ],
    };
  }

  function renderCostChart() {
    if (!costChartRef.value) return;
    costChart = costChart ?? echarts.init(costChartRef.value);
    costChart.setOption(costChartOption(), true);
  }

  function onResize() {
    costChart?.resize();
  }

  async function loadCost() {
    costLoading.value = true;
    try {
      costData.value = await platformAiQuotaCost({ groupBy: groupBy.value });
      await nextTick();
      renderCostChart();
    } finally {
      costLoading.value = false;
    }
  }

  // ==================== 模型单价 ====================
  const priceLoading = ref(false);
  const modelPrices = ref<AiModelPrice[]>([]);

  async function loadPrices() {
    priceLoading.value = true;
    try {
      modelPrices.value = await platformAiQuotaModelPriceList();
    } finally {
      priceLoading.value = false;
    }
  }

  const priceModal = reactive<{
    show: boolean;
    loading: boolean;
    title: string;
    form: {
      id?: string;
      modelCode: string;
      modelName: string;
      inputPrice: number;
      outputPrice: number;
      status: number;
    };
  }>({
    show: false,
    loading: false,
    title: '',
    form: { modelCode: '', modelName: '', inputPrice: 0, outputPrice: 0, status: 1 },
  });

  function openPriceEdit(row?: AiModelPrice) {
    priceModal.title = row ? t('platformAiQuota.editModel') : t('platformAiQuota.addModel');
    priceModal.form = row
      ? {
          id: row.id,
          modelCode: row.modelCode,
          modelName: row.modelName,
          inputPrice: Number(row.inputPrice ?? 0),
          outputPrice: Number(row.outputPrice ?? 0),
          status: row.status ?? 1,
        }
      : { modelCode: '', modelName: '', inputPrice: 0, outputPrice: 0, status: 1 };
    priceModal.show = true;
  }

  async function submitPrice() {
    const { form } = priceModal;
    if (!form.modelCode || !form.modelName) {
      Message.warning(t('platformAiQuota.modelRequired'));
      return;
    }
    priceModal.loading = true;
    try {
      await platformAiQuotaModelPriceSave({
        id: form.id,
        modelCode: form.modelCode,
        modelName: form.modelName,
        inputPrice: form.inputPrice,
        outputPrice: form.outputPrice,
        status: form.status,
      });
      Message.success(t('platformAiQuota.saveSuccess'));
      priceModal.show = false;
      await loadPrices();
    } finally {
      priceModal.loading = false;
    }
  }

  const priceColumns = computed<DataTableColumns<AiModelPrice>>(() => [
    { title: t('platformAiQuota.modelCode'), key: 'modelCode' },
    { title: t('platformAiQuota.modelName'), key: 'modelName' },
    { title: t('platformAiQuota.inputPrice'), key: 'inputPrice', render: (row) => fmtMoney(row.inputPrice) },
    { title: t('platformAiQuota.outputPrice'), key: 'outputPrice', render: (row) => fmtMoney(row.outputPrice) },
    {
      title: t('platformAiQuota.action'),
      key: 'action',
      width: 100,
      render: (row) =>
        h(
          NButton,
          { size: 'tiny', quaternary: true, type: 'primary', onClick: () => openPriceEdit(row) },
          { default: () => t('platformAiQuota.edit') }
        ),
    },
  ]);

  // ==================== 全局配置 ====================
  const config = reactive<AiQuotaConfig>({
    tokensPerCall: 10000,
    softLimitPercent: 110,
    trialQuota: 20,
    minuteCallLimit: 60,
    dailyCallLimit: 1000,
    dailyCostThreshold: 100,
  });
  const configSaving = ref(false);

  async function loadConfig() {
    const res = await platformAiQuotaGetConfig();
    Object.assign(config, res);
  }

  async function saveConfig() {
    configSaving.value = true;
    try {
      await platformAiQuotaUpdateConfig({ ...config });
      Message.success(t('platformAiQuota.saveSuccess'));
    } finally {
      configSaving.value = false;
    }
  }

  // ==================== 模拟记账（验证用） ====================
  const mock = reactive<{
    organizationId: string;
    featureCode: string;
    modelCode: string;
    inputTokens: number;
    outputTokens: number;
    loading: boolean;
    result: AiQuotaRecordResult | null;
  }>({
    organizationId: '',
    featureCode: '',
    modelCode: 'qwen',
    inputTokens: 5000,
    outputTokens: 5000,
    loading: false,
    result: null,
  });

  const featureCodeOptions = [
    'ai_advisor',
    'ai_acquire',
    'ai_sales_rag',
    'ai_video',
    'wecom_auto_analysis',
    'ai_ppt',
    'lead_crawl',
    'dm_profile',
    'ai_employee',
    'ai_kb',
    'digital_human',
  ].map((code) => ({ label: code, value: code }));

  async function submitMock() {
    if (!mock.organizationId || !mock.featureCode || !mock.modelCode) {
      Message.warning(t('platformAiQuota.mockRequired'));
      return;
    }
    mock.loading = true;
    try {
      mock.result = await platformAiQuotaMockRecord({
        organizationId: mock.organizationId,
        featureCode: mock.featureCode,
        modelCode: mock.modelCode,
        inputTokens: mock.inputTokens,
        outputTokens: mock.outputTokens,
      });
    } finally {
      mock.loading = false;
    }
  }

  onMounted(() => {
    window.addEventListener('resize', onResize);
    loadCost();
    loadPrices();
    loadConfig();
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', onResize);
    costChart?.dispose();
    costChart = null;
  });
</script>

<style lang="less" scoped>
  .paq-page {
    background: #f5f7fa;
  }
  .paq-kpi {
    padding: 18px 20px;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    background: #ffffff;
    .paq-kpi-label {
      margin-bottom: 8px;
      font-size: 13px;
      color: #8a94a6;
    }
    .paq-kpi-value {
      font-size: 24px;
      font-weight: 700;
      color: #1f2329;
      font-variant-numeric: tabular-nums;
    }
  }
  .paq-chart {
    width: 100%;
    height: 320px;
  }
  .config-row {
    display: flex;
    align-items: center;
    gap: 12px;
    .config-label {
      width: 140px;
      font-size: 13px;
      text-align: right;
      color: #4b5563;
      flex-shrink: 0;
    }
  }
</style>
