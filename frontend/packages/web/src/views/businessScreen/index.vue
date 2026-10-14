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
      @close="closePanel"
      @retry="retryDetail"
      @open-customer-list="openCustomerList"
    />

    <!-- 底部 AI 对话坞 -->
    <BusinessChat :kpis="chatKpis" />

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
  import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import dayjs from 'dayjs';

  import chinaJson from './assets/china.json';

  import type {
    BusinessScreenOverview,
    RegionDetail,
    SalesOverview,
    StageDetail,
  } from '@lib/shared/api/modules/businessScreen';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import TopShimmerBar from '@/components/pure/effects/TopShimmerBar.vue';
  import BusinessChat from './components/BusinessChat.vue';
  import DetailPanel from './components/DetailPanel.vue';
  import NeuralScene, { type SceneDimension, type SceneNode } from './components/NeuralScene.vue';

  import {
    businessScreenOverview,
    businessScreenRegionDetail,
    businessScreenSalesDetail,
    businessScreenStageDetail,
  } from '@/api/modules';
  import useOpenNewPage from '@/hooks/useOpenNewPage';

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

  const activeKey = ref<string | null>(null);
  const detailKind = ref('overview');

  const salesDetail = ref<SalesOverview | null>(null);
  const regionDetail = ref<RegionDetail | null>(null);
  const stageDetail = ref<StageDetail | null>(null);
  const detailLoading = ref(false);
  const detailFailed = ref(false);

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

  // 六个分析维度
  const dimensions = computed<SceneDimension[]>(() => [
    { key: 'overview', label: t('businessScreen.dimOverview'), color: '#4282FF' },
    { key: 'sales', label: t('businessScreen.dimSales'), color: '#37E0CC' },
    { key: 'region', label: t('businessScreen.dimRegion'), color: '#FFB958' },
    { key: 'stage', label: t('businessScreen.dimStage'), color: '#FF9562' },
    { key: 'funnel', label: t('businessScreen.dimFunnel'), color: '#7F60F9' },
    { key: 'trend', label: t('businessScreen.dimTrend'), color: '#CC71E2' },
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

  async function loadSalesDetail(ownerId: string) {
    detailLoading.value = true;
    detailFailed.value = false;
    try {
      salesDetail.value = await businessScreenSalesDetail(ownerId);
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
      regionDetail.value = await businessScreenRegionDetail(adcode);
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
      stageDetail.value = await businessScreenStageDetail(stage);
    } catch {
      stageDetail.value = null;
      detailFailed.value = true;
    } finally {
      detailLoading.value = false;
    }
  }

  function handleSelect(key: string) {
    activeKey.value = key;
    detailKind.value = resolveKind(key);

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
  }

  function retryDetail() {
    const key = activeKey.value;
    if (!key) {
      return;
    }
    handleSelect(key);
  }

  function closePanel() {
    activeKey.value = null;
    detailLoading.value = false;
    detailFailed.value = false;
  }

  async function load() {
    loading.value = true;
    failed.value = false;
    try {
      overview.value = await businessScreenOverview();
      lastRefresh.value = dayjs().format('HH:mm:ss');
    } catch {
      failed.value = true;
    } finally {
      loading.value = false;
    }
  }

  let pollTimer: number | undefined;

  onMounted(() => {
    load();
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
</style>
