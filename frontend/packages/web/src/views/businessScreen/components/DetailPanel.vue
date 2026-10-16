<template>
  <Transition name="panel">
    <aside v-if="visible" class="detail-panel">
      <header class="detail-panel__header">
        <div class="detail-panel__heading">
          <button v-if="canGoBack" class="detail-panel__back" type="button" @click="emit('back')">
            ‹ {{ t('businessScreen.back') }}
          </button>
          <span class="detail-panel__bar"></span>
          <h2 class="detail-panel__title">{{ title }}</h2>
        </div>
        <button class="detail-panel__close" type="button" aria-label="close" @click="emit('close')">×</button>
      </header>

      <div class="detail-panel__body">
        <div v-if="loading" class="detail-panel__state">
          <span>{{ t('businessScreen.loading') }}</span>
        </div>
        <div v-else-if="failed" class="detail-panel__state">
          <span>{{ t('businessScreen.loadError') }}</span>
          <button class="detail-panel__retry" type="button" @click="emit('retry')">
            {{ t('businessScreen.retry') }}
          </button>
        </div>

        <template v-else>
          <!-- 主动预警 -->
          <section v-if="kind === 'alert'" class="detail-panel__alert">
            <div v-if="alertList.length === 0" class="detail-panel__state">
              <span>{{ t('businessScreen.alertsEmpty') }}</span>
            </div>
            <button
              v-for="item in alertList"
              :key="`${item.type}-${item.entityId || item.targetKey || item.title}`"
              type="button"
              class="detail-panel__alert-card"
              @click="emit('openAlert', item)"
            >
              <div class="detail-panel__alert-head">
                <span class="detail-panel__alert-level" :class="alertLevelClass(item.level)">
                  {{ alertTypeLabel(item.type) }}
                </span>
                <span class="detail-panel__alert-go">{{ t('businessScreen.viewDetail') }} ›</span>
              </div>
              <div class="detail-panel__alert-title">{{ item.title }}</div>
              <div class="detail-panel__alert-desc">{{ item.desc }}</div>
            </button>
          </section>

          <!-- 督办看板 -->
          <section v-else-if="kind === 'supervision'" class="detail-panel__supervise">
            <form class="detail-panel__supervise-form" @submit.prevent="submitSupervision">
              <div class="detail-panel__field">
                <label class="detail-panel__label">{{ t('businessScreen.supervisionAssignee') }}</label>
                <select v-model="svAssignee" class="detail-panel__select">
                  <option value="" disabled>{{ t('businessScreen.supervisionAssignee') }}</option>
                  <option v-for="o in assigneeList" :key="o.owner" :value="o.owner">
                    {{ o.ownerName || o.owner }}
                  </option>
                </select>
              </div>
              <div class="detail-panel__field">
                <label class="detail-panel__label">{{ t('businessScreen.supervisionContent') }}</label>
                <textarea
                  v-model="svContent"
                  class="detail-panel__textarea"
                  rows="3"
                  :placeholder="t('businessScreen.supervisionContentPlaceholder')"
                ></textarea>
              </div>
              <div class="detail-panel__field">
                <label class="detail-panel__label">{{ t('businessScreen.supervisionDeadline') }}</label>
                <input v-model="svDeadline" type="datetime-local" class="detail-panel__input" />
              </div>
              <button class="detail-panel__submit" type="submit">
                {{ t('businessScreen.supervisionSubmit') }}
              </button>
            </form>

            <div v-if="supervisionList.length === 0" class="detail-panel__state detail-panel__state--sm">
              <span>{{ t('businessScreen.supervisionEmpty') }}</span>
            </div>
            <div v-for="task in supervisionList" :key="task.id" class="detail-panel__task">
              <div class="detail-panel__task-head">
                <span class="detail-panel__task-assignee">{{ task.assigneeName || task.assignee }}</span>
                <span class="detail-panel__task-status" :class="taskStatusClass(task.status)">
                  {{ taskStatusLabel(task.status) }}
                </span>
              </div>
              <div class="detail-panel__task-content">{{ task.content }}</div>
              <div class="detail-panel__task-foot">
                <span class="detail-panel__task-deadline">
                  {{ t('businessScreen.supervisionDeadline') }} {{ fmtDeadline(task.deadline) }}
                </span>
                <button
                  v-if="task.status !== 'DONE'"
                  class="detail-panel__task-done"
                  type="button"
                  @click="emit('markSupervisionDone', task.id)"
                >
                  {{ t('businessScreen.supervisionMarkDone') }}
                </button>
              </div>
            </div>
          </section>

          <!-- KPI 指标 -->
          <section v-if="kpiCards.length > 0" class="detail-panel__kpis">
            <div v-for="card in kpiCards" :key="card.label" class="detail-panel__kpi">
              <span class="detail-panel__kpi-label">{{ card.label }}</span>
              <span class="detail-panel__kpi-value" :style="{ color: card.accent }">
                {{ card.value }}<small v-if="card.unit">{{ card.unit }}</small>
              </span>
            </div>
          </section>

          <!-- AI 诊断 -->
          <section v-if="showAiDiagnosis" class="detail-panel__ai">
            <div class="detail-panel__ai-head">
              <span class="detail-panel__ai-title">{{ t('businessScreen.aiDiagnosis') }}</span>
              <button class="detail-panel__ai-regen" type="button" @click="emit('regenerateAi')">
                {{ t('businessScreen.aiDiagnosisRegenerate') }}
              </button>
            </div>
            <div v-if="aiLoading" class="detail-panel__ai-loading">
              {{ t('businessScreen.aiDiagnosisLoading') }}
            </div>
            <div v-else-if="ai" class="detail-panel__ai-body">
              <template v-if="aiSections">
                <div class="detail-panel__ai-sec">
                  <span class="detail-panel__ai-tag detail-panel__ai-tag--status">{{
                    t('businessScreen.aiDiagnosisStatus')
                  }}</span>
                  <div class="detail-panel__ai-md" v-html="aiStatusHtml"></div>
                </div>
                <div class="detail-panel__ai-sec">
                  <span class="detail-panel__ai-tag detail-panel__ai-tag--suggestion">{{
                    t('businessScreen.aiDiagnosisSuggestion')
                  }}</span>
                  <div class="detail-panel__ai-md" v-html="aiSuggestionHtml"></div>
                </div>
              </template>
              <div v-else class="detail-panel__ai-md" v-html="aiHtml"></div>
            </div>
            <div v-else class="detail-panel__ai-empty">
              {{ t('businessScreen.aiDiagnosisEmpty') }}
            </div>
          </section>

          <!-- 图表区 -->
          <section v-if="hasCharts" class="detail-panel__charts">
            <div v-if="showContractTrend" class="detail-panel__chart">
              <div class="detail-panel__chart-title">{{ t('businessScreen.contractTrend') }}</div>
              <div :ref="(el) => setChartRef('contractTrend', el)" class="detail-panel__chart-canvas"></div>
            </div>
            <div v-if="showPaymentTrend" class="detail-panel__chart">
              <div class="detail-panel__chart-title">{{ t('businessScreen.paymentTrend') }}</div>
              <div :ref="(el) => setChartRef('paymentTrend', el)" class="detail-panel__chart-canvas"></div>
            </div>
            <div v-if="showStageDist" class="detail-panel__chart">
              <div class="detail-panel__chart-title">{{ t('businessScreen.contractStage') }}</div>
              <div :ref="(el) => setChartRef('stageDist', el)" class="detail-panel__chart-canvas"></div>
            </div>
            <div v-if="showSalesRank" class="detail-panel__chart">
              <div class="detail-panel__chart-title">{{ t('businessScreen.salesRank') }}</div>
              <div :ref="(el) => setChartRef('salesRank', el)" class="detail-panel__chart-canvas"></div>
            </div>
            <div v-if="showRegionMap" class="detail-panel__chart">
              <div class="detail-panel__chart-title">{{ t('businessScreen.customerRegion') }}</div>
              <div
                :ref="(el) => setChartRef('regionMap', el)"
                class="detail-panel__chart-canvas detail-panel__chart-canvas--map"
              ></div>
            </div>
            <div v-if="showFunnel" class="detail-panel__chart">
              <div class="detail-panel__chart-title">{{ t('businessScreen.funnel') }}</div>
              <div
                :ref="(el) => setChartRef('funnel', el)"
                class="detail-panel__chart-canvas detail-panel__chart-canvas--funnel"
              ></div>
            </div>
          </section>

          <!-- 列表区 -->
          <section v-if="customerList.length > 0" class="detail-panel__list">
            <div class="detail-panel__list-head">
              <div class="detail-panel__list-title">{{ t('businessScreen.customerList') }}</div>
              <button
                v-if="canOpenCustomerList"
                class="detail-panel__list-more"
                type="button"
                @click="emit('openCustomerList')"
              >
                {{ t('businessScreen.viewMore') }} ›
              </button>
            </div>
            <div v-for="row in sortedCustomerList" :key="row.customerId || row.name" class="detail-panel__list-row">
              <span class="detail-panel__list-name">{{ row.name }}</span>
              <span class="detail-panel__list-level">{{ row.level }}</span>
              <span class="detail-panel__list-amount">{{ fmtAmount(row.contractAmount) }}</span>
            </div>
          </section>

          <section v-if="stageList.length > 0" class="detail-panel__list">
            <div class="detail-panel__list-title">{{ t('businessScreen.stageContractList') }}</div>
            <div
              v-for="row in stageList"
              :key="row.contractId"
              class="detail-panel__stage"
              @click="openContractDetail(row.contractId)"
            >
              <div class="detail-panel__stage-main">
                <span class="detail-panel__stage-name">{{ row.customerName }}</span>
                <span class="detail-panel__stage-amount"
                  >{{ fmtAmount(row.amount) }}<small>{{ t('businessScreen.unit') }}</small></span
                >
                <span class="detail-panel__stage-view">{{ t('businessScreen.viewDetail') }} ›</span>
              </div>
              <div class="detail-panel__stage-sub">
                <span class="detail-panel__stage-code"
                  >{{ t('businessScreen.contractNumber') }} {{ row.contractNumber || '-' }}</span
                >
                <span class="detail-panel__stage-owner">{{ row.ownerName || '-' }}</span>
                <span class="detail-panel__stage-paid"
                  >{{ t('businessScreen.verifiedAmount') }} {{ fmtAmount(row.verifiedAmount) }}</span
                >
                <span class="detail-panel__stage-date">{{ row.signDate || '-' }}</span>
              </div>
            </div>
          </section>

          <div v-if="empty" class="detail-panel__state">
            <span>{{ t('businessScreen.noData') }}</span>
          </div>
        </template>
      </div>
    </aside>
  </Transition>
</template>

<script setup lang="ts">
  import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
  import type { EChartsOption } from 'echarts';
  import { BarChart, FunnelChart, LineChart, MapChart, PieChart } from 'echarts/charts';
  import {
    GeoComponent,
    GridComponent,
    LegendComponent,
    TooltipComponent,
    VisualMapComponent,
  } from 'echarts/components';
  import * as echarts from 'echarts/core';
  import { CanvasRenderer } from 'echarts/renderers';

  import chinaJson from '../assets/china.json';

  import { renderMarkdown } from '@lib/shared/ai-chat';
  import type {
    AlertItem,
    AssigneeOption,
    BusinessScreenOverview,
    CustomerBrief,
    DistributionItem,
    Funnel,
    RankItem,
    RegionDetail,
    RegionItem,
    SalesOverview,
    StageContractItem,
    StageDetail,
    SupervisionItem,
    TrendPoint,
  } from '@lib/shared/api/modules/businessScreen';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import useOpenNewPage from '@/hooks/useOpenNewPage';

  import { ContractRouteEnum } from '@/enums/routeEnum';

  echarts.use([
    LineChart,
    BarChart,
    PieChart,
    FunnelChart,
    MapChart,
    GridComponent,
    LegendComponent,
    TooltipComponent,
    VisualMapComponent,
    GeoComponent,
    CanvasRenderer,
  ]);

  echarts.registerMap('china', chinaJson as Parameters<typeof echarts.registerMap>[1]);

  interface ProvinceMeta {
    name: string;
    center: [number, number];
  }

  interface GeoFeature {
    properties: {
      adcode: number | string;
      name: string;
      center?: number[];
    };
  }

  const provinceByAdcode = new Map<string, ProvinceMeta>();
  (chinaJson as unknown as { features: GeoFeature[] }).features.forEach((feature) => {
    const { center } = feature.properties;
    if (!center || center.length < 2) {
      return;
    }
    provinceByAdcode.set(String(feature.properties.adcode), {
      name: feature.properties.name,
      center: [center[0], center[1]],
    });
  });

  const props = withDefaults(
    defineProps<{
      kind: string;
      visible: boolean;
      title: string;
      loading?: boolean;
      failed?: boolean;
      overview?: BusinessScreenOverview | null;
      sales?: SalesOverview | null;
      region?: RegionDetail | null;
      stage?: StageDetail | null;
      alerts?: AlertItem[] | null;
      supervision?: SupervisionItem[] | null;
      assignees?: AssigneeOption[] | null;
      ai?: string | null;
      aiLoading?: boolean;
      canGoBack?: boolean;
    }>(),
    {
      loading: false,
      failed: false,
      overview: null,
      sales: null,
      region: null,
      stage: null,
      alerts: null,
      supervision: null,
      assignees: null,
      ai: null,
      aiLoading: false,
      canGoBack: false,
    }
  );

  const emit = defineEmits<{
    (event: 'close'): void;
    (event: 'back'): void;
    (event: 'retry'): void;
    (event: 'openCustomerList'): void;
    (event: 'regenerateAi'): void;
    (event: 'openAlert', item: AlertItem): void;
    (event: 'submitSupervision', payload: { assignee: string; content: string; deadline: number }): void;
    (event: 'markSupervisionDone', id: string): void;
  }>();

  const { t } = useI18n();
  const { openNewPage } = useOpenNewPage();

  const chartRefs: Record<string, HTMLElement | null> = {};
  const chartMap = new Map<string, echarts.ECharts>();

  const palette = ['#4282FF', '#37E0CC', '#FFB958', '#FF9562', '#7F60F9', '#CC71E2', '#76DE74', '#3BC2FC'];

  function fmtCount(v?: number): string {
    return Number(v || 0).toLocaleString('zh-CN');
  }

  function fmtAmount(v?: number): string {
    return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 });
  }

  const alertList = computed<AlertItem[]>(() => props.alerts ?? []);
  const supervisionList = computed<SupervisionItem[]>(() => props.supervision ?? []);
  const assigneeList = computed<AssigneeOption[]>(() => props.assignees ?? []);

  // 打开即显示「AI 诊断」区块：分析中 / 有结论 / 失败 三种状态在同一区块内切换，
  // 避免结论返回后区块才弹出、打开瞬间只有图表没有 AI 的观感。
  const showAiDiagnosis = computed(() => !['alert', 'supervision'].includes(props.kind));

  // 后端按「现状：… 建议：…」两段式输出；前端据此拆分出层次分明的现状 / 建议两块。
  interface AiSection {
    status: string;
    suggestion: string;
  }

  function splitAi(content: string | null | undefined): AiSection | null {
    if (!content) {
      return null;
    }
    const text = content.trim();
    const match = text.match(/建议[:：]\s*/);
    if (!match || match.index === undefined) {
      return null;
    }
    const status = text
      .slice(0, match.index)
      .replace(/^现状[:：]\s*/, '')
      .trim();
    const suggestion = text.slice(match.index + match[0].length).trim();
    if (!status && !suggestion) {
      return null;
    }
    return { status, suggestion };
  }

  const aiSections = computed(() => splitAi(props.ai));
  const aiStatusHtml = computed(() => (aiSections.value ? renderMarkdown(aiSections.value.status) : ''));
  const aiSuggestionHtml = computed(() => (aiSections.value ? renderMarkdown(aiSections.value.suggestion) : ''));
  const aiHtml = computed(() => renderMarkdown(props.ai ?? ''));

  // —— 督办表单（第一版用原生控件，避免日期浮层在玻璃面板内被裁剪） ——
  const svAssignee = ref('');
  const svContent = ref('');
  const svDeadline = ref('');

  function toDatetimeLocal(timestamp: number): string {
    const d = new Date(timestamp);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(
      d.getMinutes()
    )}`;
  }

  function fmtDeadline(timestamp: number): string {
    return toDatetimeLocal(timestamp).replace('T', ' ');
  }

  function taskStatusLabel(status: string): string {
    if (status === 'DONE') return t('businessScreen.supervisionStatusDone');
    if (status === 'OVERDUE') return t('businessScreen.supervisionStatusOverdue');
    return t('businessScreen.supervisionStatusPending');
  }

  function taskStatusClass(status: string): string {
    return `is-${status.toLowerCase()}`;
  }

  function submitSupervision(): void {
    const assignee = svAssignee.value;
    const content = svContent.value.trim();
    if (!assignee || !content || !svDeadline.value) {
      return;
    }
    const deadline = new Date(svDeadline.value).getTime();
    emit('submitSupervision', { assignee, content, deadline });
    svAssignee.value = '';
    svContent.value = '';
    svDeadline.value = '';
  }

  function alertLevelClass(level: string): string {
    return `lv-${(level || 'warning').toLowerCase()}`;
  }

  function alertTypeLabel(type: string): string {
    if (type === 'CONTRACT_DROP') return t('businessScreen.alertTypeContractDrop');
    if (type === 'STALE_BIG_CUSTOMER') return t('businessScreen.alertTypeStaleBigCustomer');
    if (type === 'STALE_OWNER') return t('businessScreen.alertTypeStaleOwner');
    return type;
  }

  const showContractTrend = computed(() => ['overview', 'trend', 'sales'].includes(props.kind));
  const showPaymentTrend = computed(() => ['overview', 'trend', 'sales'].includes(props.kind));
  const showStageDist = computed(() => ['stageDist', 'sales'].includes(props.kind));
  const showSalesRank = computed(() => ['salesRank', 'region'].includes(props.kind));
  const showRegionMap = computed(() => props.kind === 'regionMap');
  const showFunnel = computed(() => props.kind === 'funnel');
  const hasCharts = computed(
    () =>
      showContractTrend.value ||
      showPaymentTrend.value ||
      showStageDist.value ||
      showSalesRank.value ||
      showRegionMap.value ||
      showFunnel.value
  );

  interface KpiCard {
    label: string;
    value: string;
    unit: string;
    accent: string;
  }

  const kpiCards = computed<KpiCard[]>(() => {
    const unit = t('businessScreen.unit');
    if (props.kind === 'sales' && props.sales) {
      const { kpi } = props.sales;
      return [
        { label: t('businessScreen.contractAmount'), value: fmtAmount(kpi.contractAmount), unit, accent: '#4282ff' },
        { label: t('businessScreen.verifiedAmount'), value: fmtAmount(kpi.verifiedAmount), unit, accent: '#37e0cc' },
        { label: t('businessScreen.pendingAmount'), value: fmtAmount(kpi.pendingAmount), unit, accent: '#ffb958' },
        { label: t('businessScreen.contract'), value: fmtCount(kpi.contractCount), unit: '', accent: '#ff9562' },
        { label: t('businessScreen.customer'), value: fmtCount(kpi.customerCount), unit: '', accent: '#7f60f9' },
        { label: t('businessScreen.clue'), value: fmtCount(kpi.clueCount), unit: '', accent: '#3bc2fc' },
        { label: t('businessScreen.opportunity'), value: fmtCount(kpi.opportunityCount), unit: '', accent: '#76de74' },
        {
          label: t('businessScreen.monthNewCustomer'),
          value: fmtCount(kpi.monthNewCustomer),
          unit: '',
          accent: '#cc71e2',
        },
        {
          label: t('businessScreen.monthNewContract'),
          value: fmtCount(kpi.monthNewContract),
          unit: '',
          accent: '#ffb958',
        },
        {
          label: t('businessScreen.monthNewContractAmount'),
          value: fmtAmount(kpi.monthNewContractAmount),
          unit,
          accent: '#ff9562',
        },
      ];
    }
    if (props.kind === 'region' && props.region) {
      const { kpi } = props.region;
      return [
        { label: t('businessScreen.customer'), value: fmtCount(kpi.customerCount), unit: '', accent: '#4282ff' },
        { label: t('businessScreen.contract'), value: fmtCount(kpi.contractCount), unit: '', accent: '#ff9562' },
        { label: t('businessScreen.contractAmount'), value: fmtAmount(kpi.contractAmount), unit, accent: '#37e0cc' },
        { label: t('businessScreen.verifiedAmount'), value: fmtAmount(kpi.verifiedAmount), unit, accent: '#ffb958' },
        { label: t('businessScreen.pendingAmount'), value: fmtAmount(kpi.pendingAmount), unit, accent: '#7f60f9' },
      ];
    }
    if (props.kind === 'stage' && props.stage) {
      return [
        { label: t('businessScreen.contract'), value: fmtCount(props.stage.count), unit: '', accent: '#4282ff' },
        {
          label: t('businessScreen.contractAmount'),
          value: fmtAmount(props.stage.totalAmount),
          unit,
          accent: '#37e0cc',
        },
      ];
    }
    if (props.kind === 'overview' && props.overview) {
      const { kpi } = props.overview;
      return [
        { label: t('businessScreen.customer'), value: fmtCount(kpi.customerCount), unit: '', accent: '#4282ff' },
        { label: t('businessScreen.contract'), value: fmtCount(kpi.contractCount), unit: '', accent: '#ff9562' },
        {
          label: t('businessScreen.contractAmount'),
          value: fmtAmount(kpi.totalContractAmount),
          unit,
          accent: '#e94256',
        },
        { label: t('businessScreen.verifiedAmount'), value: fmtAmount(kpi.verifiedAmount), unit, accent: '#37e0cc' },
        { label: t('businessScreen.pendingAmount'), value: fmtAmount(kpi.pendingAmount), unit, accent: '#ffb958' },
      ];
    }
    return [];
  });

  const customerList = computed<CustomerBrief[]>(() => {
    if (props.kind === 'sales' && props.sales) {
      return props.sales.customerList ?? [];
    }
    if (props.kind === 'region' && props.region) {
      return props.region.customerList ?? [];
    }
    return [];
  });

  const sortedCustomerList = computed<CustomerBrief[]>(() =>
    [...customerList.value].sort((a, b) => Number(b.createTime ?? 0) - Number(a.createTime ?? 0)).slice(0, 20)
  );

  const canOpenCustomerList = computed(() => props.kind === 'sales' && !!props.sales);

  const stageList = computed<StageContractItem[]>(() => {
    if (props.kind === 'stage' && props.stage) {
      return props.stage.list ?? [];
    }
    return [];
  });

  function openContractDetail(contractId: string): void {
    openNewPage(ContractRouteEnum.CONTRACT_INDEX, { id: contractId });
  }

  const empty = computed(
    () =>
      !['alert', 'supervision'].includes(props.kind) &&
      kpiCards.value.length === 0 &&
      !hasCharts.value &&
      customerList.value.length === 0 &&
      stageList.value.length === 0
  );

  function setChartRef(key: string, el: unknown) {
    const node = (el as HTMLElement) ?? null;
    const prev = chartRefs[key];
    if (prev && prev !== node) {
      chartMap.get(key)?.dispose();
      chartMap.delete(key);
    }
    chartRefs[key] = node;
  }

  function mountChart(key: string, option: EChartsOption) {
    const el = chartRefs[key];
    if (!el) {
      return;
    }
    let chart = chartMap.get(key);
    if (!chart) {
      chart = echarts.init(el);
      chartMap.set(key, chart);
    }
    chart.setOption(option, true);
  }

  function trendOption(points: TrendPoint[]): EChartsOption {
    return {
      color: palette,
      tooltip: { trigger: 'axis' },
      grid: { left: 8, right: 16, top: 16, bottom: 4, containLabel: true },
      xAxis: {
        type: 'category',
        data: points.map((p) => p.month),
        boundaryGap: false,
        axisLabel: { color: '#c7d1e0' },
        axisLine: { lineStyle: { color: 'rgba(199,209,224,0.25)' } },
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#c7d1e0' },
        splitLine: { lineStyle: { color: 'rgba(199,209,224,0.15)', type: 'dashed' } },
      },
      series: [
        {
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          data: points.map((p) => Number(p.amount) || 0),
          lineStyle: { width: 2 },
          areaStyle: { opacity: 0.18 },
        },
      ],
    };
  }

  function pieOption(dist: DistributionItem[]): EChartsOption {
    return {
      color: palette,
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', right: 8, top: 'center', textStyle: { color: '#c7d1e0' } },
      series: [
        {
          type: 'pie',
          radius: ['42%', '66%'],
          center: ['36%', '50%'],
          data: dist.map((d) => ({ name: d.name, value: Number(d.value) || 0 })),
          label: { color: '#c7d1e0', formatter: '{b}' },
          labelLine: { lineStyle: { color: '#c7d1e0' } },
          itemStyle: { borderColor: '#0b1220', borderWidth: 2 },
        },
      ],
    };
  }

  function rankOption(rank: RankItem[]): EChartsOption {
    const names = rank.map((r) => r.ownerName || r.owner || '-');
    const values = rank.map((r) => Number(r.contractAmount) || 0);
    return {
      color: palette,
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c}' },
      grid: { left: 8, right: 24, top: 8, bottom: 8, containLabel: true },
      xAxis: {
        type: 'value',
        // 金额刻度较长，倾斜 30° 避免相邻刻度数字重叠
        axisLabel: { color: '#c7d1e0', rotate: 30 },
        splitLine: { lineStyle: { color: 'rgba(199,209,224,0.15)', type: 'dashed' } },
      },
      yAxis: {
        type: 'category',
        data: [...names].reverse(),
        axisLabel: { color: '#c7d1e0' },
        axisLine: { lineStyle: { color: 'rgba(199,209,224,0.25)' } },
      },
      series: [
        {
          type: 'bar',
          data: [...values].reverse(),
          barMaxWidth: 14,
          itemStyle: { borderRadius: [0, 4, 4, 0] },
        },
      ],
    };
  }

  function funnelOption(funnel: Funnel): EChartsOption {
    const items = [
      { name: t('businessScreen.funnelClue'), value: funnel.clue },
      { name: t('businessScreen.funnelCustomer'), value: funnel.customer },
      { name: t('businessScreen.funnelOpportunity'), value: funnel.opportunity },
      { name: t('businessScreen.funnelOrder'), value: funnel.order },
      { name: t('businessScreen.funnelContract'), value: funnel.contract },
    ];
    return {
      color: palette,
      tooltip: { trigger: 'item', formatter: '{b}: {c}' },
      series: [
        {
          type: 'funnel',
          left: '8%',
          right: '8%',
          top: 12,
          bottom: 12,
          min: 0,
          max: Math.max(...items.map((i) => Number(i.value) || 0), 1),
          sort: 'descending',
          gap: 2,
          label: { show: true, position: 'inside', color: '#0b1220', formatter: '{b} {c}' },
          data: items,
        },
      ],
    };
  }

  function mapOption(dist: RegionItem[]): EChartsOption {
    const regionUnit = t('businessScreen.customerRegionUnit');
    const rows = dist
      .map((r) => {
        const meta = provinceByAdcode.get(r.code);
        return meta ? { name: meta.name, value: Number(r.value) || 0, center: meta.center } : null;
      })
      .filter((r): r is { name: string; value: number; center: [number, number] } => r !== null);
    const maxValue = Math.max(...rows.map((r) => r.value), 1);

    return {
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(11,18,32,0.92)',
        borderColor: 'rgba(66,130,255,0.4)',
        textStyle: { color: '#e6ecf5' },
        formatter: (params: unknown) => {
          const p = params as { name?: string; value?: unknown };
          const v = Array.isArray(p.value) ? p.value[2] : p.value;
          return `${p.name ?? '-'}：${Number(v || 0)} ${regionUnit}`;
        },
      },
      visualMap: {
        min: 0,
        max: maxValue,
        left: 12,
        bottom: 12,
        text: ['高', '低'],
        calculable: false,
        inRange: { color: ['#0f1c33', '#1c3f8f', '#4282ff', '#7fd4ff'] },
        textStyle: { color: '#8ea0bd' },
      },
      geo: {
        map: 'china',
        roam: false,
        label: { show: false },
        itemStyle: { areaColor: '#0f1c33', borderColor: 'rgba(120,180,255,0.35)' },
        emphasis: { itemStyle: { areaColor: '#1c3f8f' }, label: { show: true, color: '#e6ecf5' } },
      },
      series: [
        {
          name: t('businessScreen.customer'),
          type: 'map',
          map: 'china',
          geoIndex: 0,
          data: rows.map((r) => ({ name: r.name, value: r.value })),
        },
      ],
    };
  }

  function renderCharts() {
    const { kind } = props;
    if (kind === 'overview' || kind === 'trend') {
      const data = props.overview;
      if (!data) {
        return;
      }
      mountChart('contractTrend', trendOption(data.contractTrend));
      mountChart('paymentTrend', trendOption(data.paymentTrend));
    }
    if (kind === 'sales') {
      const data = props.sales;
      if (!data) {
        return;
      }
      mountChart('contractTrend', trendOption(data.contractTrend));
      mountChart('paymentTrend', trendOption(data.paymentTrend));
      mountChart('stageDist', pieOption(data.stageDist));
    }
    if (kind === 'stageDist') {
      const data = props.overview;
      if (data) {
        mountChart('stageDist', pieOption(data.contractStageDist));
      }
    }
    if (kind === 'salesRank') {
      const data = props.overview;
      if (data) {
        mountChart('salesRank', rankOption(data.salesRank));
      }
    }
    if (kind === 'region') {
      const data = props.region;
      if (data) {
        mountChart('salesRank', rankOption(data.salesDist));
      }
    }
    if (kind === 'regionMap') {
      const data = props.overview;
      if (data) {
        mountChart('regionMap', mapOption(data.customerRegionDist));
      }
    }
    if (kind === 'funnel') {
      const data = props.overview;
      if (data) {
        mountChart('funnel', funnelOption(data.funnel));
      }
    }
  }

  watch(
    () => [props.visible, props.kind, props.overview, props.sales, props.region, props.stage] as const,
    async () => {
      await nextTick();
      renderCharts();
    },
    { deep: true }
  );

  onBeforeUnmount(() => {
    chartMap.forEach((chart) => chart.dispose());
    chartMap.clear();
  });
</script>

<style lang="less" scoped>
  .detail-panel {
    position: absolute;
    top: 12px;
    right: 12px;
    bottom: 12px;
    z-index: 5;
    display: flex;
    overflow: hidden;
    width: 420px;
    border: 1px solid rgb(120 180 255 / 18%);
    border-radius: 14px;
    background: rgb(10 17 32 / 92%);
    box-shadow: 0 12px 48px rgb(0 0 0 / 45%);
    flex-direction: column;
    backdrop-filter: blur(12px);
  }
  .detail-panel__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 16px;
    border-bottom: 1px solid rgb(120 180 255 / 12%);
  }
  .detail-panel__heading {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .detail-panel__bar {
    width: 4px;
    height: 16px;
    border-radius: 2px;
    background: linear-gradient(180deg, #4282ff, #37e0cc);
  }
  .detail-panel__title {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #e6ecf5;
  }
  .detail-panel__close {
    width: 28px;
    height: 28px;
    font-size: 18px;
    border: 1px solid rgb(255 255 255 / 12%);
    border-radius: 8px;
    color: #8ea0bd;
    background: transparent;
    cursor: pointer;
    line-height: 1;
  }
  .detail-panel__close:hover {
    border-color: rgb(120 180 255 / 40%);
    color: #ffffff;
  }
  .detail-panel__back {
    display: inline-flex;
    align-items: center;
    padding: 3px 10px;
    font-size: 12px;
    border: 1px solid rgb(120 180 255 / 25%);
    border-radius: 6px;
    white-space: nowrap;
    color: #8ea0bd;
    background: rgb(255 255 255 / 4%);
    transition: border-color 160ms ease, color 160ms ease;
    flex: none;
    cursor: pointer;
  }
  .detail-panel__back:hover {
    border-color: rgb(120 180 255 / 50%);
    color: #e6ecf5;
  }
  .detail-panel__body {
    overflow-y: auto;
    padding: 14px 16px;
    flex: 1;
  }
  .detail-panel__state {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 180px;
    font-size: 14px;
    color: #8ea0bd;
    flex-direction: column;
    gap: 12px;
  }
  .detail-panel__retry {
    padding: 6px 20px;
    font-size: 13px;
    border: 1px solid rgb(66 130 255 / 45%);
    border-radius: 6px;
    color: #c7d1e0;
    background: rgb(66 130 255 / 12%);
    cursor: pointer;
  }
  .detail-panel__kpis {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    margin-bottom: 14px;
  }
  .detail-panel__kpi {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 12px;
    border: 1px solid rgb(255 255 255 / 8%);
    border-radius: 10px;
    background: rgb(255 255 255 / 3%);
  }
  .detail-panel__kpi-label {
    font-size: 12px;
    color: #8ea0bd;
  }
  .detail-panel__kpi-value {
    font-size: 20px;
    font-weight: 700;
  }
  .detail-panel__kpi-value small {
    margin-left: 2px;
    font-size: 12px;
    font-weight: 400;
  }
  .detail-panel__charts {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .detail-panel__chart {
    padding: 12px;
    border: 1px solid rgb(255 255 255 / 8%);
    border-radius: 10px;
    background: rgb(255 255 255 / 2%);
  }
  .detail-panel__chart-title {
    margin-bottom: 8px;
    font-size: 13px;
    font-weight: 600;
    color: #c7d1e0;
  }
  .detail-panel__chart-canvas {
    width: 100%;
    height: 220px;
  }
  .detail-panel__chart-canvas--map {
    height: 280px;
  }
  .detail-panel__chart-canvas--funnel {
    height: 240px;
  }
  .detail-panel__list {
    margin-top: 14px;
  }
  .detail-panel__list-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
  }
  .detail-panel__list-title {
    margin-bottom: 8px;
    font-size: 13px;
    font-weight: 600;
    color: #c7d1e0;
  }
  .detail-panel__list-more {
    margin-bottom: 8px;
    padding: 0;
    font-size: 12px;
    border: none;
    color: #4282ff;
    background: transparent;
    transition: color 160ms ease;
    flex: none;
    line-height: 1;
    cursor: pointer;
  }
  .detail-panel__list-more:hover {
    color: #6ba0ff;
  }
  .detail-panel__list-row {
    display: flex;
    align-items: center;
    padding: 8px 10px;
    font-size: 12px;
    border-bottom: 1px solid rgb(255 255 255 / 6%);
    gap: 8px;
  }
  .detail-panel__list-name {
    flex: 1;
    color: #d6e3f7;
  }
  .detail-panel__list-level,
  .detail-panel__list-owner {
    color: #8ea0bd;
  }
  .detail-panel__list-amount {
    color: #ffb958;
    font-variant-numeric: tabular-nums;
  }
  .detail-panel__list-date {
    color: #8ea0bd;
    font-variant-numeric: tabular-nums;
  }
  .detail-panel__stage {
    padding: 10px 12px;
    border-bottom: 1px solid rgb(255 255 255 / 6%);
    cursor: pointer;
    transition: background-color 180ms ease;
  }
  .detail-panel__stage:hover {
    background: rgb(66 130 255 / 10%);
  }
  .detail-panel__stage-main {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
  }
  .detail-panel__stage-name {
    overflow: hidden;
    font-size: 13px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #d6e3f7;
  }
  .detail-panel__stage-amount {
    font-size: 13px;
    font-weight: 700;
    white-space: nowrap;
    color: #ffb958;
    font-variant-numeric: tabular-nums;
  }
  .detail-panel__stage-amount small {
    margin-left: 2px;
    font-size: 11px;
    font-weight: 400;
    color: #8ea0bd;
  }
  .detail-panel__stage-view {
    margin-left: 4px;
    font-size: 11px;
    white-space: nowrap;
    color: #37e0cc;
  }
  .detail-panel__stage-sub {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px 12px;
    margin-top: 6px;
    font-size: 11px;
    color: #8ea0bd;
  }
  .detail-panel__stage-code {
    font-family: SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
    color: #7f9ccf;
  }
  .detail-panel__stage-owner {
    color: #8ea0bd;
  }
  .detail-panel__stage-paid {
    color: #37e0cc;
    font-variant-numeric: tabular-nums;
  }
  .detail-panel__stage-date {
    color: #8ea0bd;
    font-variant-numeric: tabular-nums;
  }
  .panel-enter-active,
  .panel-leave-active {
    transition: transform 280ms cubic-bezier(0.23, 1, 0.32, 1), opacity 280ms cubic-bezier(0.23, 1, 0.32, 1);
  }
  .panel-enter-from,
  .panel-leave-to {
    transform: translateX(24px);
    opacity: 0;
  }

  // —— AI 诊断 ——
  .detail-panel__ai {
    margin-bottom: 14px;
    padding: 12px;
    border: 1px solid rgb(66 130 255 / 22%);
    border-radius: 10px;
    background: linear-gradient(135deg, rgb(66 130 255 / 10%), rgb(55 224 204 / 6%));
  }
  .detail-panel__ai-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }
  .detail-panel__ai-title {
    font-size: 13px;
    font-weight: 600;
    color: #e6ecf5;
  }
  .detail-panel__ai-regen {
    padding: 2px 10px;
    font-size: 11px;
    border: 1px solid rgb(66 130 255 / 45%);
    border-radius: 6px;
    color: #6ba0ff;
    background: rgb(66 130 255 / 10%);
    cursor: pointer;
  }
  .detail-panel__ai-regen:hover {
    color: #ffffff;
    background: rgb(66 130 255 / 20%);
  }
  .detail-panel__ai-loading {
    font-size: 13px;
    color: #8ea0bd;
    animation: detail-panel-pulse 1.2s ease-in-out infinite;
  }
  .detail-panel__ai-empty {
    font-size: 13px;
    color: #8ea0bd;
  }
  .detail-panel__ai-body {
    font-size: 13px;
    line-height: 1.7;
    color: #d6e3f7;
  }
  .detail-panel__ai-sec {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .detail-panel__ai-sec + .detail-panel__ai-sec {
    margin-top: 10px;
  }
  .detail-panel__ai-tag {
    padding: 1px 8px;
    font-size: 11px;
    font-weight: 600;
    border-radius: 6px;
    align-self: flex-start;
  }
  .detail-panel__ai-tag--status {
    color: #6ba0ff;
    background: rgb(66 130 255 / 14%);
  }
  .detail-panel__ai-tag--suggestion {
    color: #76de74;
    background: rgb(118 222 116 / 14%);
  }
  .detail-panel__ai-md {
    color: #d6e3f7;
    :deep(p) {
      margin: 0;
    }
    :deep(ol),
    :deep(ul) {
      margin: 0;
      padding-left: 18px;
    }
    :deep(ol) {
      list-style: decimal;
    }
    :deep(ul) {
      list-style: disc;
    }
    :deep(li + li) {
      margin-top: 2px;
    }
    :deep(strong) {
      font-weight: 600;
      color: #ffb958;
    }
    :deep(.ai-table-wrapper) {
      overflow-x: auto;
      margin: 6px 0;
    }
    :deep(table) {
      width: 100%;
      font-size: 12px;
      border-collapse: collapse;
      background: rgb(255 255 255 / 3%);
    }
    :deep(th),
    :deep(td) {
      padding: 6px 8px;
      border: 1px solid rgb(120 180 255 / 14%);
      text-align: left;
    }
    :deep(th) {
      color: #c7d1e0;
      background: rgb(66 130 255 / 10%);
    }
    :deep(td) {
      color: #d6e3f7;
    }
  }

  // —— 主动预警 ——
  .detail-panel__alert {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .detail-panel__alert-card {
    display: flex;
    padding: 12px;
    border: 1px solid rgb(255 255 255 / 8%);
    border-radius: 10px;
    text-align: left;
    background: rgb(255 255 255 / 3%);
    transition: border-color 160ms ease, background-color 160ms ease;
    flex-direction: column;
    gap: 6px;
    cursor: pointer;
  }
  .detail-panel__alert-card:hover {
    border-color: rgb(255 93 93 / 45%);
    background: rgb(255 93 93 / 8%);
  }
  .detail-panel__alert-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .detail-panel__alert-level {
    padding: 2px 8px;
    font-size: 11px;
    border-radius: 6px;
    color: #ffb958;
    background: rgb(255 185 88 / 12%);
  }
  .detail-panel__alert-level.lv-critical {
    color: #ff5d5d;
    background: rgb(255 93 93 / 12%);
  }
  .detail-panel__alert-go {
    font-size: 11px;
    color: #37e0cc;
  }
  .detail-panel__alert-title {
    font-size: 13px;
    font-weight: 600;
    color: #d6e3f7;
  }
  .detail-panel__alert-desc {
    font-size: 12px;
    line-height: 1.6;
    color: #8ea0bd;
  }

  // —— 督办看板 ——
  .detail-panel__supervise-form {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 16px;
    padding: 12px;
    border: 1px solid rgb(255 255 255 / 8%);
    border-radius: 10px;
    background: rgb(255 255 255 / 2%);
  }
  .detail-panel__field {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .detail-panel__label {
    font-size: 12px;
    color: #8ea0bd;
  }
  .detail-panel__select,
  .detail-panel__input,
  .detail-panel__textarea {
    padding: 8px 10px;
    width: 100%;
    font-size: 13px;
    border: 1px solid rgb(255 255 255 / 12%);
    border-radius: 8px;
    color: #d6e3f7;
    background: rgb(11 18 32 / 70%);
    outline: none;
    resize: vertical;
  }
  .detail-panel__select option {
    color: #d6e3f7;
    background: #0b1220;
  }
  .detail-panel__select:focus,
  .detail-panel__input:focus,
  .detail-panel__textarea:focus {
    border-color: rgb(66 130 255 / 55%);
  }
  .detail-panel__submit {
    align-self: flex-end;
    padding: 7px 18px;
    font-size: 13px;
    border: none;
    border-radius: 8px;
    color: #ffffff;
    background: linear-gradient(135deg, #4282ff, #37e0cc);
    cursor: pointer;
  }
  .detail-panel__state--sm {
    min-height: 80px;
  }
  .detail-panel__task {
    padding: 10px 12px;
    border-bottom: 1px solid rgb(255 255 255 / 6%);
  }
  .detail-panel__task-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
  }
  .detail-panel__task-assignee {
    font-size: 13px;
    font-weight: 600;
    color: #d6e3f7;
  }
  .detail-panel__task-status {
    flex: none;
    padding: 2px 8px;
    font-size: 11px;
    border-radius: 6px;
  }
  .detail-panel__task-status.is-pending {
    color: #ffb958;
    background: rgb(255 185 88 / 12%);
  }
  .detail-panel__task-status.is-done {
    color: #37e0cc;
    background: rgb(55 224 204 / 12%);
  }
  .detail-panel__task-status.is-overdue {
    color: #ff5d5d;
    background: rgb(255 93 93 / 12%);
  }
  .detail-panel__task-content {
    margin-top: 6px;
    font-size: 13px;
    line-height: 1.6;
    color: #d6e3f7;
  }
  .detail-panel__task-foot {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    margin-top: 6px;
  }
  .detail-panel__task-deadline {
    font-size: 11px;
    color: #8ea0bd;
    font-variant-numeric: tabular-nums;
  }
  .detail-panel__task-done {
    flex: none;
    padding: 3px 12px;
    font-size: 12px;
    border: 1px solid rgb(55 224 204 / 45%);
    border-radius: 6px;
    color: #37e0cc;
    background: rgb(55 224 204 / 10%);
    cursor: pointer;
  }
  .detail-panel__task-done:hover {
    color: #ffffff;
    background: rgb(55 224 204 / 22%);
  }

  @keyframes detail-panel-pulse {
    0%,
    100% {
      opacity: 0.4;
    }
    50% {
      opacity: 1;
    }
  }
</style>
