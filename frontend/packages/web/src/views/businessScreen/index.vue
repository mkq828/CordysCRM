<template>
  <div class="business-screen">
    <div class="bs-particles">
      <ParticleBackground color="#4da3ff" :count="70" />
    </div>

    <TopShimmerBar :height="3" />

    <!-- 头部：标题 + 时钟 -->
    <header class="bs-header">
      <div class="bs-header__left">
        <CrmIcon type="iconicon_precise_monitor" :size="28" color="#4da3ff" />
        <h1 class="bs-header__title">{{ t('businessScreen.title') }}</h1>
      </div>
      <div class="bs-header__right">
        <FlipClock :value="clockStr" />
        <div class="bs-header__meta">
          <span class="bs-header__date">{{ dateStr }}</span>
          <span class="bs-header__refresh">{{ t('businessScreen.refreshHint', { time: lastRefresh }) }}</span>
        </div>
      </div>
    </header>

    <!-- 光球 + 水母场景 -->
    <div class="bs-stage">
      <NeuralScene
        :dimensions="dimensions"
        :children-by-dimension="childrenByDimension"
        :center-stats="centerStats"
        :active-key="activeKey"
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
    />

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
  import FlipClock from '@/components/pure/effects/FlipClock.vue';
  import ParticleBackground from '@/components/pure/effects/ParticleBackground.vue';
  import TopShimmerBar from '@/components/pure/effects/TopShimmerBar.vue';
  import DetailPanel from './components/DetailPanel.vue';
  import NeuralScene, { type SceneDimension, type SceneNode } from './components/NeuralScene.vue';

  import {
    businessScreenOverview,
    businessScreenRegionDetail,
    businessScreenSalesDetail,
    businessScreenStageDetail,
  } from '@/api/modules';

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

  // 时钟
  const now = ref(new Date());
  const clockStr = computed(() => dayjs(now.value).format('HH:mm:ss'));
  const dateStr = computed(() => dayjs(now.value).format('YYYY-MM-DD'));

  function fmtCount(v?: number): string {
    return Number(v || 0).toLocaleString('zh-CN');
  }

  function fmtAmount(v?: number): string {
    return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 });
  }

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

  let clockTimer: number | undefined;
  let pollTimer: number | undefined;

  onMounted(() => {
    load();
    clockTimer = window.setInterval(() => {
      now.value = new Date();
    }, 1000);
    pollTimer = window.setInterval(() => load(), 30000);
  });

  onBeforeUnmount(() => {
    if (clockTimer !== undefined) {
      window.clearInterval(clockTimer);
    }
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
    color: #e6ecf5;
    background: linear-gradient(160deg, #0b1220 0%, #0f1c33 55%, #0b1220 100%);
  }
  .bs-particles {
    position: absolute;
    inset: 0;
    z-index: 0;
    opacity: 0.5;
    pointer-events: none;
  }
  .bs-header {
    position: absolute;
    top: 0;
    right: 0;
    left: 0;
    z-index: 4;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    gap: 24px;
    pointer-events: none;
  }
  .bs-header__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .bs-header__title {
    margin: 0;
    font-size: 26px;
    font-weight: 700;
    letter-spacing: 2px;
    color: #e6ecf5;
  }
  .bs-header__right {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .bs-header__meta {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .bs-header__date {
    font-size: 14px;
    color: #c7d1e0;
  }
  .bs-header__refresh {
    font-size: 12px;
    color: #8ea0bd;
  }
  .bs-stage {
    position: absolute;
    inset: 0;
    z-index: 1;
  }
  .bs-state {
    position: absolute;
    z-index: 6;
    display: flex;
    justify-content: center;
    align-items: center;
    background: rgb(11 18 32 / 60%);
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
