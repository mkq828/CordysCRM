<template>
  <div class="askbi-page">
    <!-- 页头 -->
    <div class="askbi-head">
      <h1 class="askbi-title">{{ t('askBi.title') }}</h1>
      <p class="askbi-desc">{{ t('askBi.desc') }}</p>
    </div>

    <!-- 未开通 -->
    <n-empty v-if="available === false" class="askbi-locked" :description="t('askBi.locked')" />

    <template v-else>
      <!-- 搜索区 -->
      <div class="askbi-search">
        <div class="askbi-search-row">
          <n-input
            v-model:value="question"
            size="large"
            clearable
            :placeholder="t('askBi.placeholder')"
            @keydown.enter="handleQuery"
          />
          <AiActionButton :loading="loading" :disabled="!question.trim() || loading" @click="handleQuery">
            {{ loading ? t('askBi.querying') : t('askBi.query') }}
          </AiActionButton>
        </div>

        <div v-if="sampleQuestions.length" class="askbi-samples">
          <button v-for="q in sampleQuestions" :key="q" type="button" class="askbi-sample" @click="applySample(q)">
            {{ q }}
          </button>
        </div>

        <p v-if="scopeHint" class="askbi-scope">{{ scopeHint }}</p>
      </div>

      <!-- 生成中 -->
      <div v-if="loading" class="askbi-loading">
        <ShimmerText size="14px">{{ t('askBi.querying') }}</ShimmerText>
      </div>

      <!-- 结果 -->
      <div v-if="result && !loading" class="askbi-result">
        <h3 class="askbi-result-title">{{ result.title }}</h3>

        <!-- 单值卡片 -->
        <div v-if="chartType === 'number'" class="askbi-number">
          <span class="askbi-number-value">{{ fmtNumber(numberValue) }}</span>
          <span class="askbi-number-label">{{ columns[0] }}</span>
        </div>

        <!-- 柱状图 -->
        <div v-else-if="chartType === 'bar'" class="askbi-bars">
          <div v-for="(r, i) in chartRows" :key="i" class="askbi-bar">
            <span class="askbi-bar-label" :title="r.label">{{ r.label }}</span>
            <div class="askbi-bar-track">
              <div class="askbi-bar-fill" :style="{ width: barPct(r.value) }"></div>
            </div>
            <span class="askbi-bar-value">{{ fmtNumber(r.value) }}</span>
          </div>
        </div>

        <!-- 折线图 -->
        <div v-else-if="chartType === 'line'" class="askbi-line-wrap">
          <svg class="askbi-line" :viewBox="`0 0 ${LINE_W} ${LINE_H}`" preserveAspectRatio="none">
            <polyline :points="linePoints" fill="none" stroke="#f97316" stroke-width="2" />
            <circle v-for="(p, i) in lineDots" :key="i" :cx="p.x" :cy="p.y" r="3" fill="#f97316" />
          </svg>
          <div class="askbi-line-labels">
            <span v-for="(r, i) in chartRows" :key="i" class="askbi-line-label">{{ r.label }}</span>
          </div>
        </div>

        <!-- 饼图 -->
        <div v-else-if="chartType === 'pie'" class="askbi-pie-wrap">
          <div class="askbi-pie" :style="pieStyle">
            <div class="askbi-pie-hole"></div>
          </div>
          <ul class="askbi-legend">
            <li v-for="(r, i) in chartRows" :key="i" class="askbi-legend-item">
              <i class="askbi-legend-dot" :style="{ background: PIE_COLORS[i % PIE_COLORS.length] }"></i>
              <span class="askbi-legend-label">{{ r.label }}</span>
              <span class="askbi-legend-value">{{ fmtNumber(r.value) }}</span>
            </li>
          </ul>
        </div>

        <!-- 表格 -->
        <n-data-table
          v-if="rows.length"
          class="askbi-table"
          :columns="tableColumns"
          :data="tableData"
          :bordered="false"
          size="small"
        />
        <n-empty v-else-if="result" class="askbi-empty" :description="t('askBi.empty')" />
      </div>

      <!-- 无结果占位 -->
      <div v-if="!result && !loading && available" class="askbi-hint">{{ t('askBi.hint') }}</div>
    </template>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  import { NDataTable, NEmpty, NInput, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AskBiCatalogResult, AskBiQueryResult } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  import { askBiQuery, getAiBiAvailable, getAiBiCatalog } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const available = ref<boolean | null>(null);
  const catalog = ref<AskBiCatalogResult | null>(null);
  const question = ref('');
  const loading = ref(false);
  const result = ref<AskBiQueryResult | null>(null);

  const sampleQuestions = computed(() => catalog.value?.sampleQuestions ?? []);
  const scopeHint = computed(() => catalog.value?.scopeHint ?? '');

  const chartType = computed(() => result.value?.chartType ?? 'table');
  const columns = computed(() => result.value?.columns ?? []);
  const rows = computed(() => result.value?.rows ?? []);

  const numberValue = computed(() => (rows.value.length ? rows.value[0]?.[0] : null));

  const chartRows = computed(() =>
    rows.value.map((r) => ({ label: String(r?.[0] ?? ''), value: Number(r?.[1] ?? 0) }))
  );
  const maxValue = computed(() => Math.max(1, ...chartRows.value.map((r) => r.value)));

  const LINE_W = 360;
  const LINE_H = 150;
  const PAD = 20;

  const lineDots = computed(() => {
    const rs = chartRows.value;
    if (!rs.length) return [];
    const stepX = rs.length > 1 ? (LINE_W - PAD * 2) / (rs.length - 1) : 0;
    return rs.map((r, i) => ({
      x: PAD + stepX * i,
      y: LINE_H - PAD - (r.value / maxValue.value) * (LINE_H - PAD * 2),
      label: r.label,
      value: r.value,
    }));
  });
  const linePoints = computed(() => lineDots.value.map((p) => `${p.x},${p.y}`).join(' '));

  const PIE_COLORS = ['#f97316', '#0ea5e9', '#8b5cf6', '#22c55e', '#ef4444', '#eab308', '#14b8a6', '#f43f5e'];
  const pieStyle = computed(() => {
    const rs = chartRows.value;
    const total = rs.reduce((s, r) => s + r.value, 0);
    if (total <= 0) return {};
    let acc = 0;
    const stops = rs.map((r, i) => {
      const start = (acc / total) * 360;
      acc += r.value;
      const end = (acc / total) * 360;
      return `${PIE_COLORS[i % PIE_COLORS.length]} ${start.toFixed(2)}deg ${end.toFixed(2)}deg`;
    });
    return { background: `conic-gradient(${stops.join(', ')})` };
  });

  function fmtNumber(v: unknown): string | number {
    if (v === null || v === undefined) return '-';
    if (typeof v === 'number') {
      return Number.isInteger(v) ? v.toLocaleString('zh-CN') : v.toLocaleString('zh-CN', { maximumFractionDigits: 2 });
    }
    return String(v);
  }

  const tableColumns = computed(() => columns.value.map((c, i) => ({ title: c, key: `c${i}` })));
  const tableData = computed(() =>
    rows.value.map((r) => {
      const o: Record<string, string | number> = {};
      (r ?? []).forEach((v, i) => {
        o[`c${i}`] = fmtNumber(v);
      });
      return o;
    })
  );

  function barPct(v: number): string {
    return `${Math.round((v / maxValue.value) * 100)}%`;
  }

  function applySample(q: string) {
    question.value = q;
  }

  async function handleQuery() {
    const q = question.value.trim();
    if (!q || loading.value) return;
    loading.value = true;
    result.value = null;
    try {
      result.value = await askBiQuery({ question: q });
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      loading.value = false;
    }
  }

  onMounted(async () => {
    try {
      available.value = await getAiBiAvailable();
      if (available.value) {
        catalog.value = await getAiBiCatalog();
      }
    } catch {
      available.value = false;
    }
  });
</script>

<style scoped>
  .askbi-page {
    overflow: auto;
    padding: 24px;
    height: 100%;
  }
  .askbi-head {
    margin-bottom: 20px;
  }
  .askbi-title {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    color: var(--text-n1, #1f2937);
  }
  .askbi-desc {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--text-n2, #6b7280);
  }
  .askbi-locked {
    margin-top: 60px;
  }
  .askbi-search {
    padding: 20px;
    border-radius: 12px;
    background: #ffffff;
    box-shadow: 0 1px 3px rgb(0 0 0 / 6%);
  }
  .askbi-search-row {
    display: flex;
    gap: 12px;
    align-items: center;
  }
  .askbi-search-row :deep(.n-input) {
    flex: 1;
  }
  .askbi-samples {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 14px;
  }
  .askbi-sample {
    padding: 5px 12px;
    font-size: 12px;
    border: 1px solid #fed7aa;
    border-radius: 999px;
    color: #ea580c;
    background: #fff7ed;
    transition: all 0.15s;
    cursor: pointer;
  }
  .askbi-sample:hover {
    border-color: #fdba74;
    background: #ffedd5;
  }
  .askbi-scope {
    margin: 12px 0 0;
    font-size: 12px;
    white-space: pre-line;
    color: #f97316;
    line-height: 1.8;
  }
  .askbi-loading {
    margin-top: 20px;
    padding: 20px;
    border-radius: 12px;
    text-align: center;
    background: #ffffff;
  }
  .askbi-result {
    margin-top: 20px;
    padding: 20px;
    border-radius: 12px;
    background: #ffffff;
    box-shadow: 0 1px 3px rgb(0 0 0 / 6%);
  }
  .askbi-result-title {
    margin: 0 0 16px;
    font-size: 16px;
    font-weight: 600;
    color: var(--text-n1, #1f2937);
  }
  .askbi-number {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 28px 0;
  }
  .askbi-number-value {
    font-size: 40px;
    font-weight: 700;
    color: #f97316;
  }
  .askbi-number-label {
    font-size: 13px;
    color: var(--text-n2, #6b7280);
  }
  .askbi-bars {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .askbi-bar {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .askbi-bar-label {
    overflow: hidden;
    width: 140px;
    font-size: 12px;
    text-align: right;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-n2, #6b7280);
    flex-shrink: 0;
  }
  .askbi-bar-track {
    overflow: hidden;
    height: 18px;
    border-radius: 4px;
    background: #f3f4f6;
    flex: 1;
  }
  .askbi-bar-fill {
    height: 100%;
    border-radius: 4px;
    background: linear-gradient(90deg, #fb923c, #f97316);
  }
  .askbi-bar-value {
    width: 100px;
    flex-shrink: 0;
    font-size: 12px;
    color: var(--text-n1, #1f2937);
  }
  .askbi-line-wrap {
    display: flex;
    flex-direction: column;
  }
  .askbi-line {
    width: 100%;
    height: 150px;
  }
  .askbi-line-labels {
    display: flex;
    justify-content: space-between;
    margin-top: 4px;
  }
  .askbi-line-label {
    overflow: hidden;
    min-width: 0;
    font-size: 11px;
    text-align: center;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-n2, #6b7280);
    flex: 1;
  }
  .askbi-pie-wrap {
    display: flex;
    align-items: center;
    gap: 24px;
    flex-wrap: wrap;
  }
  .askbi-pie {
    position: relative;
    width: 160px;
    height: 160px;
    flex-shrink: 0;
    border-radius: 50%;
  }
  .askbi-pie-hole {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: #ffffff;
    transform: translate(-50%, -50%);
  }
  .askbi-legend {
    display: flex;
    margin: 0;
    padding: 0;
    min-width: 200px;
    list-style: none;
    flex-direction: column;
    gap: 8px;
  }
  .askbi-legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
  }
  .askbi-legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 2px;
    flex-shrink: 0;
  }
  .askbi-legend-label {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-n2, #6b7280);
  }
  .askbi-legend-value {
    color: var(--text-n1, #1f2937);
  }
  .askbi-table {
    margin-top: 16px;
  }
  .askbi-empty {
    margin-top: 20px;
  }
  .askbi-hint {
    margin-top: 20px;
    padding: 32px 20px;
    font-size: 13px;
    border-radius: 12px;
    text-align: center;
    color: #f97316;
    background: #ffffff;
  }
</style>
