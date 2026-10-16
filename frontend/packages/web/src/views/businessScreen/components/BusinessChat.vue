<template>
  <div ref="rootEl" class="bs-chat" :class="{ 'bs-chat--collapsed': !expanded }">
    <Transition name="bs-chat-fade">
      <section v-if="expanded && cards" class="bs-chat__results" aria-live="polite">
        <header class="bs-chat__results-head">
          <span class="bs-chat__results-title">{{ t('businessScreen.chat.title') }}</span>
          <small>{{ t('businessScreen.chat.localHint') }}</small>
          <button type="button" class="bs-chat__close" aria-label="close" @click="cards = null">×</button>
        </header>
        <div class="bs-chat__cards">
          <!-- 生成中 -->
          <div v-if="loading" class="bs-chat__loading">
            <ShimmerText size="13px">{{ t('businessScreen.chat.querying') }}</ShimmerText>
          </div>

          <!-- 出错 -->
          <article v-else-if="cards.error" class="bs-chat__card">
            <h4>{{ cards.q }}</h4>
            <p class="bs-chat__error">{{ cards.error }}</p>
          </article>

          <!-- 结构化结果 -->
          <article v-else-if="cards.result" class="bs-chat__card bs-chat__result">
            <h4>{{ cards.result.title || cards.q }}</h4>
            <div v-if="rows.length" class="bs-chat__chart">
              <!-- 单值 -->
              <div v-if="chartType === 'number'" class="bs-chat__number">
                <span class="bs-chat__number-value">{{ fmtNumber(numberValue) }}</span>
                <span class="bs-chat__number-label">{{ columns[0] }}</span>
              </div>

              <!-- 柱状图 -->
              <div v-else-if="chartType === 'bar'" class="bs-chat__bars">
                <div v-for="(r, i) in chartRows" :key="i" class="bs-chat__bar">
                  <span class="bs-chat__bar-label" :title="r.label">{{ r.label }}</span>
                  <div class="bs-chat__bar-track">
                    <div class="bs-chat__bar-fill" :style="{ width: barPct(r.value) }"></div>
                  </div>
                  <span class="bs-chat__bar-value">{{ fmtNumber(r.value) }}</span>
                </div>
              </div>

              <!-- 折线图 -->
              <div v-else-if="chartType === 'line'" class="bs-chat__line-wrap">
                <svg class="bs-chat__line" :viewBox="`0 0 ${LINE_W} ${LINE_H}`" preserveAspectRatio="none">
                  <polyline :points="linePoints" fill="none" stroke="#f97316" stroke-width="2" />
                  <circle v-for="(p, i) in lineDots" :key="i" :cx="p.x" :cy="p.y" r="3" fill="#f97316" />
                </svg>
                <div class="bs-chat__line-labels">
                  <span v-for="(r, i) in chartRows" :key="i" class="bs-chat__line-label">{{ r.label }}</span>
                </div>
              </div>

              <!-- 饼图 -->
              <div v-else-if="chartType === 'pie'" class="bs-chat__pie-wrap">
                <div class="bs-chat__pie" :style="pieStyle">
                  <div class="bs-chat__pie-hole"></div>
                </div>
                <ul class="bs-chat__legend">
                  <li v-for="(r, i) in chartRows" :key="i" class="bs-chat__legend-item">
                    <i class="bs-chat__legend-dot" :style="{ background: PIE_COLORS[i % PIE_COLORS.length] }"></i>
                    <span class="bs-chat__legend-label">{{ r.label }}</span>
                    <span class="bs-chat__legend-value">{{ fmtNumber(r.value) }}</span>
                  </li>
                </ul>
              </div>

              <!-- 表格 -->
              <table v-else class="bs-chat__table">
                <thead v-if="columns.length">
                  <tr>
                    <th v-for="(c, i) in columns" :key="i">{{ c }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(r, i) in rows" :key="i">
                    <td v-for="(v, j) in r" :key="j">{{ fmtNumber(v) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="bs-chat__empty">{{ t('businessScreen.chat.noData') }}</div>
          </article>
        </div>
      </section>
    </Transition>

    <div class="bs-chat__panel">
      <div v-if="expanded" class="bs-chat__kpis">
        <div v-for="kpi in kpis" :key="kpi.label" class="bs-chat__kpi">
          <span>{{ kpi.label }}</span>
          <strong>{{ kpi.value }}</strong>
        </div>
      </div>
      <form class="bs-chat__ask" @submit.prevent="submit">
        <input
          v-model="q"
          type="text"
          :placeholder="t('businessScreen.chat.placeholder')"
          aria-label="向经营数据提问"
          @focus="expanded = true"
        />
        <button type="submit" class="bs-chat__send" :disabled="loading" aria-label="提交问题">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M5 12h13M13 6l6 6-6 6" />
          </svg>
        </button>
      </form>
      <div v-if="expanded" class="bs-chat__chips">
        <span class="bs-chat__me">{{ t('businessScreen.chat.me') }}</span>
        <button v-for="chip in chips" :key="chip" type="button" @click="typeAsk(chip)">
          {{ chip }}
        </button>
        <button class="bs-chat__supervise" type="button" @click="emit('supervise')">
          {{ t('businessScreen.supervisionEntry') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AskBiQueryResult } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';

  import { askBiScreenQuery } from '@/api/modules';

  export interface ChatKpi {
    label: string;
    value: string;
  }

  const props = withDefaults(defineProps<{ kpis?: ChatKpi[] }>(), { kpis: () => [] });

  const emit = defineEmits<{ (event: 'supervise'): void }>();

  const { t } = useI18n();

  // 与后端 BiCatalog.SAMPLE_QUESTIONS 保持一致（大屏免费全租户，不依赖企业版 catalog 接口）。
  const SAMPLE_QUESTIONS = [
    '上月各客户回款金额 Top10',
    '本月新签合同额是多少',
    '近 6 个月每月回款趋势',
    '各负责人已核销回款金额',
    '今年新增客户数',
  ];

  const loading = ref(false);
  const q = ref('');
  const cards = ref<{ q: string; result?: AskBiQueryResult; error?: string } | null>(null);
  const expanded = ref(false);
  const rootEl = ref<HTMLElement | null>(null);
  let typingTimer: number | undefined;

  const chips = SAMPLE_QUESTIONS;

  const chartType = computed(() => cards.value?.result?.chartType ?? 'table');
  const columns = computed(() => cards.value?.result?.columns ?? []);
  const rows = computed(() => cards.value?.result?.rows ?? []);

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

  function barPct(v: number): string {
    return `${Math.round((v / maxValue.value) * 100)}%`;
  }

  async function ask(text: string): Promise<void> {
    const value = text.trim();
    if (!value || loading.value) {
      return;
    }
    expanded.value = true;
    loading.value = true;
    cards.value = { q: value };
    try {
      const result = await askBiScreenQuery({ question: value });
      cards.value = { q: value, result };
    } catch (error) {
      cards.value = { q: value, error: (error as Error)?.message || t('common.operationFailed') };
    } finally {
      loading.value = false;
    }
  }

  function typeAsk(text: string): void {
    if (typingTimer !== undefined) {
      window.clearInterval(typingTimer);
    }
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      q.value = text;
      ask(text);
      return;
    }
    let i = 0;
    q.value = '';
    typingTimer = window.setInterval(() => {
      i += 1;
      q.value = text.slice(0, i);
      if (i >= text.length) {
        window.clearInterval(typingTimer);
        window.setTimeout(() => ask(text), 220);
      }
    }, 45);
  }

  function submit(): void {
    ask(q.value);
  }

  function onDocumentPointerDown(event: PointerEvent): void {
    if (rootEl.value && !rootEl.value.contains(event.target as Node)) {
      expanded.value = false;
    }
  }

  onMounted(() => {
    document.addEventListener('pointerdown', onDocumentPointerDown);
  });

  onBeforeUnmount(() => {
    if (typingTimer !== undefined) {
      window.clearInterval(typingTimer);
    }
    document.removeEventListener('pointerdown', onDocumentPointerDown);
  });
</script>

<style scoped lang="less">
  .bs-chat {
    position: absolute;
    bottom: 22px;
    left: 50%;
    z-index: 4;
    display: grid;
    width: min(600px, calc(100vw - 32px));
    gap: 10px;
    transform: translateX(-50%);
  }
  .bs-chat__panel {
    display: flex;
    flex-direction: column;
  }
  .bs-chat:not(.bs-chat--collapsed) .bs-chat__panel {
    padding: 10px 12px;
    border: 1px solid rgb(236 236 232 / 12%);
    border-radius: 24px;
    background: linear-gradient(160deg, rgb(52 53 52 / 50%), rgb(14 15 15 / 70%));
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 8%), 0 24px 60px rgb(0 0 0 / 45%);
    backdrop-filter: blur(20px) saturate(1.3);
  }
  @keyframes bs-panel-beam {
    to {
      filter: hue-rotate(360deg);
    }
  }
  .bs-chat__kpis {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    padding: 2px 4px 10px;
  }
  .bs-chat__kpi {
    display: grid;
    gap: 1px;
    padding: 0 10px;
    border-left: 1px solid rgb(232 232 226 / 11%);
  }
  .bs-chat__kpi:first-child {
    padding-left: 4px;
    border-left: 0;
  }
  .bs-chat__kpi span {
    font-size: 10.5px;
    white-space: nowrap;
    color: #a5a6a1;
  }
  .bs-chat__kpi strong {
    font-size: 19px;
    font-weight: 600;
    color: #ecece9;
  }
  .bs-chat__ask {
    position: relative;
    display: flex;
    align-items: center;
    padding: 4px;
    border-radius: 999px;
    background: rgb(18 19 19 / 80%);
    box-shadow: 0 14px 40px rgb(0 0 0 / 42%);
    backdrop-filter: blur(16px) saturate(1.2);
  }
  .bs-chat__ask::before {
    position: absolute;
    z-index: -1;
    padding: 1.5px;
    border-radius: inherit;
    background: conic-gradient(from 0deg, #f59e0b, #ec4899, #8b5cf6, #3b82f6, #10b981, #f59e0b);
    opacity: 0.5;
    content: '';
    inset: 0;
    mask: linear-gradient(#000000 0 0) content-box, linear-gradient(#000000 0 0);
    mask-composite: exclude;
    pointer-events: none;
    animation: bs-panel-beam 6s linear infinite;
  }
  .bs-chat__ask input {
    padding: 0 44px 0 14px;
    width: 100%;
    height: 38px;
    border: 0;
    border-radius: 999px;
    color: #ecece9;
    background: transparent;
    outline: none;
  }
  .bs-chat__ask input:focus {
    box-shadow: none;
  }
  .bs-chat__ask input::placeholder {
    color: #75766f;
  }
  .bs-chat__ask input:disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }
  .bs-chat__send {
    position: absolute;
    right: 6px;
    display: grid;
    width: 30px;
    height: 30px;
    border: 0;
    border-radius: 50%;
    color: #a5a6a1;
    background: rgb(255 255 255 / 8%);
    place-items: center;
    cursor: pointer;
  }
  .bs-chat__send:hover {
    color: #ecece9;
    background: rgb(255 255 255 / 14%);
  }
  .bs-chat__send:disabled {
    cursor: not-allowed;
    opacity: 0.45;
  }
  .bs-chat__chips {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
    align-items: center;
    padding: 9px 2px 0;
  }
  .bs-chat__me {
    display: inline-flex;
    align-items: center;
    padding: 4px 9px;
    font-size: 11px;
    border-radius: 8px;
    color: #a5a6a1;
    background: rgb(255 255 255 / 5%);
    gap: 5px;
  }
  .bs-chat__chips button {
    padding: 4px 10px;
    font-size: 11px;
    border: 1px solid rgb(232 232 226 / 11%);
    border-radius: 999px;
    color: #a5a6a1;
    background: none;
    cursor: pointer;
  }
  .bs-chat__chips button:hover {
    border-color: rgb(236 236 232 / 35%);
    color: #ecece9;
    background: rgb(236 236 232 / 7%);
  }
  .bs-chat__chips button:disabled {
    cursor: not-allowed;
    opacity: 0.5;
  }
  .bs-chat__chips button.bs-chat__supervise {
    margin-left: auto;
    border-color: rgb(230 207 152 / 35%);
    color: #e6cf98;
    background: rgb(230 207 152 / 8%);
  }
  .bs-chat__chips button.bs-chat__supervise:hover {
    border-color: rgb(236 236 232 / 35%);
    color: #ecece9;
    background: rgb(230 207 152 / 18%);
  }
  .bs-chat__results {
    overflow: auto;
    padding: 12px;
    max-height: min(46vh, 420px);
    border: 1px solid rgb(232 232 226 / 11%);
    border-radius: 16px;
    background: rgb(22 23 23 / 90%);
    box-shadow: 0 24px 60px rgb(0 0 0 / 45%);
  }
  .bs-chat__results-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 10px;
    font-size: 12px;
    font-weight: 600;
  }
  .bs-chat__results-title {
    color: #ecece9;
  }
  .bs-chat__results-head small {
    font-size: 11px;
    font-weight: 400;
    color: #75766f;
  }
  .bs-chat__close {
    margin-left: auto;
    width: 24px;
    height: 24px;
    border: 1px solid rgb(232 232 226 / 11%);
    border-radius: 50%;
    color: #a5a6a1;
    background: rgb(255 255 255 / 3%);
    cursor: pointer;
  }
  .bs-chat__cards {
    display: grid;
    gap: 8px;
  }
  .bs-chat__card {
    position: relative;
    padding: 10px 12px 11px 16px;
    border: 1px solid rgb(232 232 226 / 11%);
    border-radius: 12px;
    background: rgb(255 255 255 / 3%);
  }
  .bs-chat__card::before {
    position: absolute;
    top: 12px;
    bottom: 12px;
    left: 7px;
    width: 2px;
    border-radius: 2px;
    background: #ace0cf;
    content: '';
  }
  .bs-chat__card h4 {
    margin: 0 0 3px;
    font-size: 13px;
    font-weight: 600;
    color: #ecece9;
  }
  .bs-chat__card p {
    margin: 0;
    font-size: 12px;
    color: #a5a6a1;
  }
  .bs-chat__loading {
    display: grid;
    padding: 20px 12px;
    place-items: center;
  }
  .bs-chat__error {
    color: #f97316 !important;
  }
  .bs-chat__result h4 {
    margin-bottom: 12px;
  }
  .bs-chat__chart {
    display: grid;
    gap: 10px;
  }
  .bs-chat__number {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 18px 0;
  }
  .bs-chat__number-value {
    font-size: 34px;
    font-weight: 700;
    color: #f97316;
  }
  .bs-chat__number-label {
    font-size: 12px;
    color: #a5a6a1;
  }
  .bs-chat__bars {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .bs-chat__bar {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .bs-chat__bar-label {
    overflow: hidden;
    width: 120px;
    font-size: 11px;
    text-align: right;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #a5a6a1;
    flex-shrink: 0;
  }
  .bs-chat__bar-track {
    overflow: hidden;
    height: 14px;
    border-radius: 4px;
    background: rgb(255 255 255 / 8%);
    flex: 1;
  }
  .bs-chat__bar-fill {
    height: 100%;
    border-radius: 4px;
    background: linear-gradient(90deg, #fb923c, #f97316);
  }
  .bs-chat__bar-value {
    width: 88px;
    flex-shrink: 0;
    font-size: 11px;
    color: #ecece9;
  }
  .bs-chat__line-wrap {
    display: flex;
    flex-direction: column;
  }
  .bs-chat__line {
    width: 100%;
    height: 130px;
  }
  .bs-chat__line-labels {
    display: flex;
    justify-content: space-between;
    margin-top: 4px;
  }
  .bs-chat__line-label {
    overflow: hidden;
    min-width: 0;
    font-size: 10px;
    text-align: center;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #a5a6a1;
    flex: 1;
  }
  .bs-chat__pie-wrap {
    display: flex;
    align-items: center;
    gap: 18px;
    flex-wrap: wrap;
  }
  .bs-chat__pie {
    position: relative;
    width: 130px;
    height: 130px;
    flex-shrink: 0;
    border-radius: 50%;
  }
  .bs-chat__pie-hole {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 66px;
    height: 66px;
    border-radius: 50%;
    background: rgb(22 23 23);
    transform: translate(-50%, -50%);
  }
  .bs-chat__legend {
    display: flex;
    margin: 0;
    padding: 0;
    min-width: 160px;
    list-style: none;
    flex-direction: column;
    gap: 6px;
  }
  .bs-chat__legend-item {
    display: flex;
    align-items: center;
    gap: 7px;
    font-size: 11px;
  }
  .bs-chat__legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 2px;
    flex-shrink: 0;
  }
  .bs-chat__legend-label {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #a5a6a1;
  }
  .bs-chat__legend-value {
    color: #ecece9;
  }
  .bs-chat__table {
    width: 100%;
    font-size: 11px;
    border-collapse: collapse;
  }
  .bs-chat__table th,
  .bs-chat__table td {
    padding: 7px 10px;
    border-bottom: 1px solid rgb(232 232 226 / 8%);
    text-align: left;
    white-space: nowrap;
  }
  .bs-chat__table th {
    font-weight: 600;
    color: #ecece9;
    background: rgb(255 255 255 / 4%);
  }
  .bs-chat__table td {
    color: #a5a6a1;
  }
  .bs-chat__table tbody tr:last-child td {
    border-bottom: 0;
  }
  .bs-chat__empty {
    padding: 18px 12px;
    font-size: 12px;
    text-align: center;
    color: #a5a6a1;
  }
  .bs-chat-fade-enter-active,
  .bs-chat-fade-leave-active {
    transition: opacity 0.3s, transform 0.3s;
  }
  .bs-chat-fade-enter-from,
  .bs-chat-fade-leave-to {
    opacity: 0;
    transform: translateY(10px);
  }
</style>
