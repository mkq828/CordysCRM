<template>
  <div class="business-screen">
    <TopShimmerBar :height="3" />

    <!-- 顶部导航：返回工作台 / AI 经营决策中枢 -->
    <header class="bs-topbar">
      <div class="bs-brand">
        <span class="bs-brand__mark">
          <CrmIcon type="iconicon_precise_monitor" :size="18" color="#d8d9d4" />
        </span>
        <span class="bs-brand__text">{{ t('businessScreen.title') }}</span>
      </div>
      <nav class="bs-nav" aria-label="主导航">
        <button type="button" class="bs-nav__item" @click="goBack">
          {{ t('businessScreen.backToWorkbench') }}
          <em>{{ t('businessScreen.backToWorkbenchSub') }}</em>
        </button>
        <button type="button" class="bs-nav__item active" aria-current="page">
          {{ t('businessScreen.title') }}
          <em>{{ t('businessScreen.titleSub') }}</em>
        </button>
      </nav>
      <div class="bs-topbar__meta">
        <span class="bs-topbar__date">{{ dateStr }}</span>
        <span class="bs-topbar__refresh">{{ t('businessScreen.refreshHint', { time: lastRefresh }) }}</span>
      </div>
    </header>

    <!-- AI 经营简报卡（首屏右上角，可收起） -->
    <aside class="bs-briefing" :class="{ 'bs-briefing--collapsed': briefingCollapsed }">
      <header class="bs-briefing__head">
        <span class="bs-briefing__title">{{ t('businessScreen.briefing') }}</span>
        <button class="bs-briefing__toggle" type="button" @click="briefingCollapsed = !briefingCollapsed">
          {{ briefingCollapsed ? '＋' : '－' }}
        </button>
      </header>
      <div v-if="!briefingCollapsed" class="bs-briefing__body">
        <div v-if="briefingLoading" class="bs-briefing__loading">
          <ShimmerText size="12px">{{ t('businessScreen.briefingGenerating') }}</ShimmerText>
        </div>
        <div v-else-if="briefing" class="bs-briefing__content">
          <div class="bs-briefing__meta">
            <span class="bs-briefing__type">{{ briefingTypeLabel(briefing.type) }}</span>
            <span class="bs-briefing__period">{{ briefing.period }}</span>
          </div>
          <div class="bs-briefing__text" v-html="briefingHtml"></div>
        </div>
        <div v-else class="bs-briefing__empty">{{ t('businessScreen.briefingEmpty') }}</div>
        <div class="bs-briefing__foot">
          <button class="bs-briefing__gen" type="button" :disabled="briefingLoading" @click="generateBriefing('DAILY')">
            {{ t('businessScreen.briefingGenerate') }}
          </button>
        </div>
      </div>
    </aside>

    <!-- 时间范围切换（左侧竖排：累计 → 今日） -->
    <aside class="bs-range" aria-label="统计时间范围">
      <button
        v-for="opt in rangeOptions"
        :key="opt.code"
        type="button"
        class="bs-range__item"
        :class="{ 'bs-range__item--active': range === opt.code }"
        @click="selectRange(opt.code)"
      >
        {{ t(opt.key) }}
      </button>
    </aside>

    <!-- 光球 + 水母场景 -->
    <div class="bs-stage">
      <NeuralScene
        :dimensions="dimensions"
        :children-by-dimension="childrenByDimension"
        :center-stats="centerStats"
        :active-key="activeKey"
        :panel-visible="panelVisible"
        @select="handleSelect"
      />
    </div>

    <!-- 右侧详情面板 -->
    <DetailPanel
      :kind="detailKind"
      :visible="panelVisible"
      :title="detailTitle"
      :loading="detailLoading"
      :failed="detailFailed"
      :overview="overview"
      :sales="salesDetail"
      :region="regionDetail"
      :stage="stageDetail"
      :alerts="alerts"
      :supervision="supervision"
      :assignees="assignees"
      :ai="ai"
      :ai-loading="aiLoading"
      :can-go-back="canGoBack"
      @close="closePanel"
      @retry="retryDetail"
      @open-customer-list="openCustomerList"
      @regenerate-ai="handleRegenerateAi"
      @open-alert="openAlert"
      @submit-supervision="submitSupervision"
      @mark-supervision-done="markSupervisionDone"
      @back="handleBack"
    />

    <!-- 底部 AI 对话坞 -->
    <BusinessChat :kpis="chatKpis" @supervise="openSupervision" />

    <!-- 加载 / 错误态 -->
    <div v-if="!overview && loading" class="bs-state">
      <span class="bs-state__text">{{ t('businessScreen.loading') }}</span>
    </div>
    <div v-else-if="!overview && failed" class="bs-state">
      <span class="bs-state__text">{{ t('businessScreen.loadError') }}</span>
      <button class="bs-retry" type="button" @click="load()">{{ t('businessScreen.retry') }}</button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import dayjs from 'dayjs';

  import chinaJson from './assets/china.json';

  import { renderMarkdown } from '@lib/shared/ai-chat';
  import type {
    AlertItem,
    AssigneeOption,
    BriefingResponse,
    BusinessScreenOverview,
    RegionDetail,
    SalesOverview,
    StageDetail,
    SupervisionItem,
  } from '@lib/shared/api/modules/businessScreen';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import TopShimmerBar from '@/components/pure/effects/TopShimmerBar.vue';
  import BusinessChat from './components/BusinessChat.vue';
  import DetailPanel from './components/DetailPanel.vue';
  import NeuralScene, { type SceneDimension, type SceneNode } from './components/NeuralScene.vue';

  import {
    businessScreenAlerts,
    businessScreenAttribution,
    businessScreenBriefingGenerate,
    businessScreenBriefingLatest,
    businessScreenOverview,
    businessScreenRegionDetail,
    businessScreenSalesDetail,
    businessScreenStageDetail,
    businessScreenSupervisionBoard,
    businessScreenSupervisionCreate,
    businessScreenSupervisionDone,
  } from '@/api/modules';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import useTypewriter from '@/hooks/useTypewriter';

  import { CustomerRouteEnum, WorkbenchRouteEnum } from '@/enums/routeEnum';

  interface GeoFeature {
    properties: {
      adcode: number | string;
      name: string;
    };
  }

  const provinceNameByAdcode = new Map<string, string>();
  (chinaJson as unknown as { features: GeoFeature[] }).features.forEach((feature) => {
    provinceNameByAdcode.set(String(feature.properties.adcode), feature.properties.name);
  });

  const { t } = useI18n();

  const router = useRouter();
  const { openNewPage } = useOpenNewPage();

  function goBack(): void {
    router.push({ name: WorkbenchRouteEnum.WORKBENCH_BOARD });
  }

  const loading = ref(false);
  const failed = ref(false);
  const overview = ref<BusinessScreenOverview | null>(null);
  const lastRefresh = ref('--');

  type RangeCode = 'today' | 'week' | 'month' | 'quarter' | 'year' | 'all';
  const range = ref<RangeCode>('all');
  const rangeOptions: { code: RangeCode; key: string }[] = [
    { code: 'all', key: 'businessScreen.rangeAll' },
    { code: 'year', key: 'businessScreen.rangeYear' },
    { code: 'quarter', key: 'businessScreen.rangeQuarter' },
    { code: 'month', key: 'businessScreen.rangeMonth' },
    { code: 'week', key: 'businessScreen.rangeWeek' },
    { code: 'today', key: 'businessScreen.rangeToday' },
  ];

  function selectRange(code: RangeCode): void {
    range.value = code;
  }

  const activeKey = ref<string | null>(null);
  const detailKind = ref('overview');
  // 返回目标：下钻到子节点/预警明细时记录其父级（第一层），返回按钮只回到当前水母的第一层
  const parentKey = ref<string | null>(null);
  const canGoBack = computed(() => parentKey.value !== null);

  const salesDetail = ref<SalesOverview | null>(null);
  const regionDetail = ref<RegionDetail | null>(null);
  const stageDetail = ref<StageDetail | null>(null);
  const detailLoading = ref(false);
  const detailFailed = ref(false);

  const alerts = ref<AlertItem[] | null>(null);
  const ai = ref<string | null>(null);
  const aiLoading = ref(false);

  // 归因结论逐字输出：POST 一次性返回后 feed 全文，按节拍逐字揭示；首拍揭示时结束「正在分析」
  const aiReqId = ref(0);
  const aiTypewriter = useTypewriter({
    onTick: () => {
      aiLoading.value = false;
    },
  });
  watch(
    aiTypewriter.text,
    (value) => {
      ai.value = value || null;
    },
    { flush: 'sync' }
  );
  const supervision = ref<SupervisionItem[]>([]);
  const assignees = ref<AssigneeOption[]>([]);
  const briefing = ref<BriefingResponse | null>(null);
  const briefingLoading = ref(false);
  const briefingCollapsed = ref(false);
  const briefingHtml = computed(() => renderMarkdown(briefing.value?.content ?? ''));

  function openCustomerList(): void {
    const key = activeKey.value;
    if (!key?.startsWith('sales:')) {
      return;
    }
    const ownerId = key.slice(6);
    const rank = overview.value?.salesRank.find((item) => item.owner === ownerId);
    const ownerName = rank?.ownerName || rank?.owner || ownerId;
    openNewPage(CustomerRouteEnum.CUSTOMER_INDEX, { ownerId, ownerName });
  }

  // 时钟
  const dateStr = dayjs().format('YYYY-MM-DD');

  function fmtCount(v?: number): string {
    return Number(v || 0).toLocaleString('zh-CN');
  }

  function fmtAmount(v?: number): string {
    return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 });
  }

  const chatKpis = computed(() => {
    const kpi = overview.value?.kpi;
    return [
      { label: t('businessScreen.customer'), value: fmtCount(kpi?.customerCount) },
      { label: t('businessScreen.contract'), value: fmtCount(kpi?.contractCount) },
      { label: t('businessScreen.contractAmount'), value: fmtAmount(kpi?.totalContractAmount) },
      { label: t('businessScreen.verifiedAmount'), value: fmtAmount(kpi?.verifiedAmount) },
    ];
  });

  // 七个分析维度（第 7 个为「主动预警」）
  const dimensions = computed<SceneDimension[]>(() => [
    { key: 'overview', label: t('businessScreen.dimOverview'), color: '#4282FF' },
    { key: 'sales', label: t('businessScreen.dimSales'), color: '#37E0CC' },
    { key: 'region', label: t('businessScreen.dimRegion'), color: '#FFB958' },
    { key: 'stage', label: t('businessScreen.dimStage'), color: '#FF9562' },
    { key: 'funnel', label: t('businessScreen.dimFunnel'), color: '#7F60F9' },
    { key: 'trend', label: t('businessScreen.dimTrend'), color: '#CC71E2' },
    { key: 'alert', label: t('businessScreen.dimAlert'), color: '#FF5D5D' },
  ]);

  // 各维度展开后的二级节点
  const childrenByDimension = computed<Record<string, SceneNode[]>>(() => {
    const result: Record<string, SceneNode[]> = {};
    const data = overview.value;
    if (!data) {
      return result;
    }
    result.sales = data.salesRank.map((rank) => ({
      key: `sales:${rank.owner}`,
      label: rank.ownerName || rank.owner || '-',
      color: '#37E0CC',
    }));
    result.region = data.customerRegionDist.map((item) => ({
      key: `region:${item.code}`,
      label: provinceNameByAdcode.get(item.code) ?? item.code,
      color: '#FFB958',
    }));
    result.stage = data.contractStageDist.map((item) => ({
      key: `stage:${item.code ?? item.name}`,
      label: item.name,
      color: '#FF9562',
    }));
    return result;
  });

  // 中心光球：企业总览核心指标
  const centerStats = computed(() => {
    const kpi = overview.value?.kpi;
    return {
      key: 'overview',
      title: t('businessScreen.title'),
      primaryValue: `${fmtAmount(kpi?.totalContractAmount)} ${t('businessScreen.unit')}`,
      subLabel: t('businessScreen.orbSub', {
        verified: fmtAmount(kpi?.verifiedAmount),
        customers: fmtCount(kpi?.customerCount),
      }),
    };
  });

  const panelVisible = computed(() => activeKey.value !== null);

  const detailTitle = computed(() => {
    const key = activeKey.value;
    if (!key) {
      return '';
    }
    if (key === 'overview') {
      return t('businessScreen.dimOverview');
    }
    if (key === 'sales') {
      return t('businessScreen.salesRank');
    }
    if (key === 'region') {
      return t('businessScreen.customerRegion');
    }
    if (key === 'stage') {
      return t('businessScreen.contractStage');
    }
    if (key === 'funnel') {
      return t('businessScreen.funnel');
    }
    if (key === 'trend') {
      return t('businessScreen.contractTrend');
    }
    if (key === 'alert') {
      return t('businessScreen.dimAlert');
    }
    if (key === 'supervision') {
      return t('businessScreen.supervision');
    }
    if (key.startsWith('sales:')) {
      const owner = key.slice(6);
      const rank = overview.value?.salesRank.find((item) => item.owner === owner);
      return rank?.ownerName || rank?.owner || '-';
    }
    if (key.startsWith('region:')) {
      const adcode = key.slice(7);
      return provinceNameByAdcode.get(adcode) ?? adcode;
    }
    if (key.startsWith('stage:')) {
      const code = key.slice(6);
      const dist = overview.value?.contractStageDist.find((item) => (item.code ?? item.name) === code);
      return dist?.name ?? code;
    }
    return key;
  });

  function resolveKind(key: string): string {
    if (key === 'overview') {
      return 'overview';
    }
    if (key === 'sales') {
      return 'salesRank';
    }
    if (key === 'region') {
      return 'regionMap';
    }
    if (key === 'stage') {
      return 'stageDist';
    }
    if (key === 'funnel') {
      return 'funnel';
    }
    if (key === 'trend') {
      return 'trend';
    }
    if (key === 'alert') {
      return 'alert';
    }
    if (key === 'supervision') {
      return 'supervision';
    }
    if (key.startsWith('sales:')) {
      return 'sales';
    }
    if (key.startsWith('region:')) {
      return 'region';
    }
    if (key.startsWith('stage:')) {
      return 'stage';
    }
    return 'overview';
  }

  function bareKey(key: string, kind: string): string {
    if (kind === 'sales') {
      return key.slice(6);
    }
    if (kind === 'region') {
      return key.slice(7);
    }
    if (kind === 'stage') {
      return key.slice(6);
    }
    return key;
  }

  async function loadSalesDetail(ownerId: string) {
    detailLoading.value = true;
    detailFailed.value = false;
    try {
      salesDetail.value = await businessScreenSalesDetail(ownerId, range.value);
    } catch {
      salesDetail.value = null;
      detailFailed.value = true;
    } finally {
      detailLoading.value = false;
    }
  }

  async function loadRegionDetail(adcode: string) {
    detailLoading.value = true;
    detailFailed.value = false;
    try {
      regionDetail.value = await businessScreenRegionDetail(adcode, range.value);
    } catch {
      regionDetail.value = null;
      detailFailed.value = true;
    } finally {
      detailLoading.value = false;
    }
  }

  async function loadStageDetail(stage: string) {
    detailLoading.value = true;
    detailFailed.value = false;
    try {
      stageDetail.value = await businessScreenStageDetail(stage, range.value);
    } catch {
      stageDetail.value = null;
      detailFailed.value = true;
    } finally {
      detailLoading.value = false;
    }
  }

  async function loadAlerts() {
    try {
      const res = await businessScreenAlerts();
      alerts.value = res.items;
    } catch {
      alerts.value = [];
    }
  }

  async function loadSupervision() {
    try {
      const res = await businessScreenSupervisionBoard();
      supervision.value = res.items;
      assignees.value = res.assignees;
    } catch {
      supervision.value = [];
      assignees.value = [];
    }
  }

  async function loadAttribution(key: string) {
    const reqId = ++aiReqId.value;
    aiLoading.value = true;
    aiTypewriter.reset();
    ai.value = null;
    try {
      const kind = resolveKind(key);
      const res = await businessScreenAttribution({ kind, key: bareKey(key, kind), range: range.value });
      if (reqId !== aiReqId.value) {
        return;
      }
      aiTypewriter.feed(res.content);
      await aiTypewriter.finish();
    } catch {
      if (reqId === aiReqId.value) {
        ai.value = null;
      }
    } finally {
      if (reqId === aiReqId.value) {
        aiLoading.value = false;
      }
    }
  }

  function applySelect(key: string) {
    activeKey.value = key;
    detailKind.value = resolveKind(key);

    if (key === 'alert') {
      detailLoading.value = false;
      detailFailed.value = false;
      loadAlerts();
      return;
    }
    if (key === 'supervision') {
      detailLoading.value = false;
      detailFailed.value = false;
      loadSupervision();
      return;
    }

    if (key.startsWith('sales:')) {
      loadSalesDetail(key.slice(6));
    } else if (key.startsWith('region:')) {
      loadRegionDetail(key.slice(7));
    } else if (key.startsWith('stage:')) {
      loadStageDetail(key.slice(6));
    } else {
      detailLoading.value = false;
      detailFailed.value = false;
    }

    loadAttribution(key);
  }

  // 子节点 key（sales:xxx / region:xxx / stage:xxx）对应的父级根节点；根节点或非下钻 key 返回 null
  function rootOf(key: string): string | null {
    if (key.startsWith('sales:')) {
      return 'sales';
    }
    if (key.startsWith('region:')) {
      return 'region';
    }
    if (key.startsWith('stage:')) {
      return 'stage';
    }
    return null;
  }

  function handleSelect(key: string) {
    // 水母节点点击：只有下钻到子节点才记返回目标（父级第一层），切到根节点则清空返回
    parentKey.value = rootOf(key);
    applySelect(key);
  }

  function handleBack() {
    const prev = parentKey.value;
    if (prev === null) {
      return;
    }
    parentKey.value = null;
    applySelect(prev);
  }

  function retryDetail() {
    const key = activeKey.value;
    if (!key) {
      return;
    }
    applySelect(key);
  }

  function closePanel() {
    activeKey.value = null;
    detailLoading.value = false;
    detailFailed.value = false;
    parentKey.value = null;
  }

  async function submitSupervision(payload: { assignee: string; content: string; deadline: number }) {
    try {
      await businessScreenSupervisionCreate(payload);
      await loadSupervision();
    } catch {
      // 保持现有列表不变，避免玻璃面板上抛未捕获错误
    }
  }

  async function markSupervisionDone(id: string) {
    try {
      await businessScreenSupervisionDone(id);
      await loadSupervision();
    } catch {
      // 同上
    }
  }

  function openSupervision() {
    handleSelect('supervision');
  }

  function handleRegenerateAi() {
    const key = activeKey.value;
    if (!key) {
      return;
    }
    loadAttribution(key);
  }

  function openAlert(item: AlertItem) {
    // 从预警卡片下钻到明细，返回目标固定为预警列表（第一层）
    parentKey.value = 'alert';
    if (item.targetKind === 'trend') {
      applySelect('trend');
      return;
    }
    if (item.targetKind === 'sales' && item.targetKey) {
      applySelect(`sales:${item.targetKey}`);
      return;
    }
    applySelect('overview');
  }

  function briefingTypeLabel(type: string): string {
    return type === 'WEEKLY' ? t('businessScreen.briefingTypeWeekly') : t('businessScreen.briefingTypeDaily');
  }

  async function loadBriefing() {
    try {
      briefing.value = await businessScreenBriefingLatest();
    } catch {
      briefing.value = null;
    }
  }

  async function generateBriefing(type: string) {
    if (briefingLoading.value) {
      return;
    }
    briefingLoading.value = true;
    try {
      briefing.value = await businessScreenBriefingGenerate(type);
    } catch {
      // 生成失败保持原简报
    } finally {
      briefingLoading.value = false;
    }
  }

  async function load() {
    loading.value = true;
    failed.value = false;
    try {
      overview.value = await businessScreenOverview(range.value);
      lastRefresh.value = dayjs().format('HH:mm:ss');
    } catch {
      failed.value = true;
    } finally {
      loading.value = false;
    }
  }

  // 切档后若已打开某个下钻明细，同步按新窗口刷新明细与归因
  function reloadDetail() {
    const key = activeKey.value;
    if (!key) {
      return;
    }
    if (key === 'alert') {
      loadAlerts();
      return;
    }
    if (key === 'supervision') {
      loadSupervision();
      return;
    }
    if (key.startsWith('sales:')) {
      loadSalesDetail(key.slice(6));
    } else if (key.startsWith('region:')) {
      loadRegionDetail(key.slice(7));
    } else if (key.startsWith('stage:')) {
      loadStageDetail(key.slice(6));
    }
    loadAttribution(key);
  }

  // 切换时间范围后重新拉取总览（watch 只在实际值变化时触发，重复点击同一项不会重复请求）
  watch(range, () => {
    load();
    reloadDetail();
  });

  let pollTimer: number | undefined;

  onMounted(() => {
    load();
    loadBriefing();
    pollTimer = window.setInterval(() => load(), 30000);
  });

  onBeforeUnmount(() => {
    if (pollTimer !== undefined) {
      window.clearInterval(pollTimer);
    }
  });
</script>

<style lang="less" scoped>
  .business-screen {
    position: relative;
    overflow: hidden;
    height: 100%;
    color: #e8e9e4;
    background: radial-gradient(
        ellipse 55% 60% at 50% 52%,
        rgb(88 90 88 / 24%),
        rgb(30 31 31 / 26%) 45%,
        transparent 75%
      ),
      radial-gradient(ellipse 120% 90% at 50% 50%, #141515 0%, #090a0a 72%);
  }
  .bs-topbar {
    position: absolute;
    top: 0;
    right: 0;
    left: 0;
    z-index: 4;
    display: grid;
    align-items: center;
    padding: 0 28px;
    height: 64px;
    background: linear-gradient(to bottom, rgb(12 13 13 / 92%), rgb(12 13 13 / 0%));
    grid-template-columns: 1fr auto 1fr;
    pointer-events: none;
  }
  .bs-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    color: #e8e9e4;
  }
  .bs-brand__mark {
    display: grid;
    width: 32px;
    height: 32px;
    border: 1px solid rgb(236 236 232 / 22%);
    border-radius: 10px;
    background: rgb(236 236 232 / 7%);
    box-shadow: 0 0 18px rgb(236 236 232 / 10%);
    place-items: center;
  }
  .bs-brand__text {
    font-size: 14px;
    letter-spacing: 0.14em;
    font-weight: 600;
  }
  .bs-nav {
    display: flex;
    padding: 4px;
    border: 1px solid rgb(232 232 226 / 11%);
    border-radius: 999px;
    background: rgb(26 27 27 / 62%);
    gap: 4px;
    backdrop-filter: blur(12px);
    pointer-events: auto;
  }
  .bs-nav__item {
    position: relative;
    display: grid;
    padding: 6px 16px;
    font-size: 13px;
    border: 0;
    border-radius: 999px;
    white-space: nowrap;
    color: #a5a6a1;
    background: none;
    transition: color 0.2s, background 0.2s;
    cursor: pointer;
    justify-items: center;
    line-height: 1.2;
  }
  .bs-nav__item em {
    margin: 1px 0 0;
    font-size: 9.5px;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    font-style: normal;
  }
  .bs-nav__item.active em {
    color: #e6cf98;
  }
  .bs-nav__item:hover {
    color: #e8e9e4;
  }
  .bs-nav__item.active {
    color: #e8e9e4;
    background: linear-gradient(180deg, rgb(236 236 232 / 14%), rgb(236 236 232 / 4%));
    box-shadow: inset 0 0 0 1px rgb(236 236 232 / 26%), 0 0 22px rgb(236 236 232 / 8%);
  }
  .bs-topbar__meta {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
  }
  .bs-topbar__date {
    font-size: 14px;
    color: #c7d1e0;
  }
  .bs-topbar__refresh {
    font-size: 12px;
    color: #9a9c96;
  }
  .bs-stage {
    position: absolute;
    inset: 0;
  }
  .bs-state {
    position: absolute;
    z-index: 6;
    display: flex;
    justify-content: center;
    align-items: center;
    background: rgb(10 11 11 / 72%);
    inset: 0;
    flex-direction: column;
    gap: 16px;
  }
  .bs-state__text {
    font-size: 16px;
    color: #8ea0bd;
  }
  .bs-retry {
    padding: 6px 20px;
    font-size: 13px;
    border: 1px solid rgb(66 130 255 / 45%);
    border-radius: 6px;
    color: #c7d1e0;
    background: rgb(66 130 255 / 12%);
    cursor: pointer;
  }
  .bs-retry:hover {
    border-color: rgb(100 180 255 / 70%);
    background: rgb(66 130 255 / 22%);
  }
  .bs-briefing {
    position: absolute;
    top: 136px;
    right: 24px;
    z-index: 4;
    display: flex;
    overflow: hidden;
    width: 320px;
    max-height: calc(100vh - 190px);
    border: 1px solid rgb(236 236 232 / 14%);
    border-radius: 16px;
    background: linear-gradient(160deg, rgb(52 53 52 / 60%), rgb(14 15 15 / 78%));
    box-shadow: 0 24px 60px rgb(0 0 0 / 45%);
    backdrop-filter: blur(20px) saturate(1.3);
    flex-direction: column;
  }
  .bs-briefing--collapsed {
    width: auto;
  }
  .bs-briefing__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 14px;
    border-bottom: 1px solid rgb(236 236 232 / 10%);
  }
  .bs-briefing__title {
    font-size: 13px;
    font-weight: 600;
    color: #ecece9;
  }
  .bs-briefing__toggle {
    width: 22px;
    height: 22px;
    font-size: 13px;
    border: 1px solid rgb(236 236 232 / 14%);
    border-radius: 6px;
    color: #a5a6a1;
    background: rgb(255 255 255 / 4%);
    line-height: 1;
    cursor: pointer;
  }
  .bs-briefing__toggle:hover {
    color: #ecece9;
  }
  .bs-briefing__body {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 12px 14px;
  }
  .bs-briefing__loading {
    display: flex;
    justify-content: center;
    padding: 12px 0;
  }
  .bs-briefing__empty {
    padding: 12px 0;
    font-size: 12px;
    text-align: center;
    color: #75766f;
  }
  .bs-briefing__content {
    display: flex;
    overflow-y: auto;
    max-height: calc(100vh - 350px);
    flex-direction: column;
    gap: 6px;
  }
  .bs-briefing__meta {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .bs-briefing__type {
    padding: 2px 8px;
    font-size: 11px;
    border-radius: 6px;
    color: #e6cf98;
    background: rgb(230 207 152 / 12%);
  }
  .bs-briefing__period {
    font-size: 11px;
    color: #75766f;
  }
  .bs-briefing__text {
    margin: 0;
    font-size: 12px;
    color: #c7d1e0;
    line-height: 1.7;
    :deep(p) {
      margin: 0 0 6px;
    }
    :deep(p:last-child) {
      margin-bottom: 0;
    }
    :deep(ol),
    :deep(ul) {
      margin: 0;
      padding-left: 16px;
    }
    :deep(ol) {
      list-style: decimal;
    }
    :deep(ul) {
      list-style: disc;
    }
    :deep(li + li) {
      margin-top: 3px;
    }
    :deep(strong) {
      font-weight: 600;
      color: #e6cf98;
    }
    :deep(.ai-table-wrapper) {
      overflow-x: auto;
      margin: 6px 0;
    }
    :deep(table) {
      width: 100%;
      font-size: 11px;
      border-collapse: collapse;
      background: rgb(255 255 255 / 3%);
    }
    :deep(th),
    :deep(td) {
      padding: 5px 7px;
      border: 1px solid rgb(236 236 232 / 12%);
      text-align: left;
    }
    :deep(th) {
      color: #ecece9;
      background: rgb(255 255 255 / 4%);
    }
    :deep(td) {
      color: #c7d1e0;
    }
  }
  .bs-briefing__foot {
    display: flex;
    justify-content: flex-end;
  }
  .bs-briefing__gen {
    padding: 6px 16px;
    font-size: 12px;
    border: 1px solid rgb(230 207 152 / 35%);
    border-radius: 999px;
    color: #e6cf98;
    background: rgb(230 207 152 / 8%);
    cursor: pointer;
  }
  .bs-briefing__gen:hover {
    color: #ffffff;
    background: rgb(230 207 152 / 18%);
  }
  .bs-briefing__gen:disabled {
    cursor: not-allowed;
    opacity: 0.5;
  }
  .bs-range {
    position: absolute;
    top: 88px;
    left: 24px;
    z-index: 4;
    display: flex;
    padding: 4px;
    border: 1px solid rgb(236 236 232 / 12%);
    border-radius: 14px;
    background: linear-gradient(160deg, rgb(52 53 52 / 55%), rgb(14 15 15 / 70%));
    box-shadow: 0 18px 44px rgb(0 0 0 / 38%);
    backdrop-filter: blur(18px) saturate(1.2);
    flex-direction: column;
    gap: 2px;
  }
  .bs-range__item {
    padding: 7px 14px;
    font-size: 12.5px;
    border: 0;
    border-radius: 10px;
    text-align: center;
    white-space: nowrap;
    color: #a5a6a1;
    background: none;
    transition: color 0.2s, background 0.2s;
    cursor: pointer;
  }
  .bs-range__item:hover {
    color: #e8e9e4;
  }
  .bs-range__item--active {
    color: #e6cf98;
    background: linear-gradient(180deg, rgb(236 236 232 / 14%), rgb(236 236 232 / 4%));
    box-shadow: inset 0 0 0 1px rgb(236 236 232 / 24%);
  }
</style>
