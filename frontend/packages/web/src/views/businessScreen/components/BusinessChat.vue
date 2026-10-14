<template>
  <div class="bs-chat">
    <Transition name="bs-chat-fade">
      <section v-if="cards" class="bs-chat__results" aria-live="polite">
        <header class="bs-chat__results-head">
          <span class="bs-chat__results-title">{{ t('businessScreen.chat.title') }}</span>
          <small>{{ t('businessScreen.chat.localHint') }}</small>
          <button type="button" class="bs-chat__close" aria-label="close" @click="cards = null">×</button>
        </header>
        <div class="bs-chat__cards">
          <article class="bs-chat__card">
            <h4>{{ cards.q }}</h4>
            <p>{{ cards.answer }}</p>
          </article>
        </div>
      </section>
    </Transition>

    <div class="bs-chat__panel">
      <div class="bs-chat__kpis">
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
        />
        <button type="submit" class="bs-chat__send" aria-label="提交问题">
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
      <div class="bs-chat__chips">
        <span class="bs-chat__me">{{ t('businessScreen.chat.me') }}</span>
        <button v-for="chip in chips" :key="chip" type="button" @click="typeAsk(chip)">{{ chip }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { onBeforeUnmount, ref } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  export interface ChatKpi {
    label: string;
    value: string;
  }

  const props = withDefaults(defineProps<{ kpis?: ChatKpi[] }>(), { kpis: () => [] });

  const emit = defineEmits<{
    (event: 'ask', text: string): void;
  }>();

  const { t } = useI18n();

  const chips = [
    t('businessScreen.chat.q1'),
    t('businessScreen.chat.q2'),
    t('businessScreen.chat.q3'),
    t('businessScreen.chat.q4'),
  ];

  const q = ref('');
  const cards = ref<{ q: string; answer: string } | null>(null);
  let typingTimer: number | undefined;

  function ask(text: string): void {
    const value = text.trim();
    if (!value) {
      return;
    }
    // 先接一个占位回答，后续在此处接入系统 AI。
    cards.value = { q: value, answer: t('businessScreen.chat.empty') };
    emit('ask', value);
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

  onBeforeUnmount(() => {
    if (typingTimer !== undefined) {
      window.clearInterval(typingTimer);
    }
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
    position: relative;
    padding: 10px 12px;
    border: 0;
    border-radius: 24px;
    background: linear-gradient(160deg, rgb(52 53 52 / 50%), rgb(14 15 15 / 70%));
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 8%), inset 0 0 32px rgb(255 255 255 / 4%), 0 24px 60px rgb(0 0 0 / 45%),
      0 0 80px rgb(139 92 246 / 15%);
    backdrop-filter: blur(20px) saturate(1.3);
  }
  .bs-chat__panel::before {
    position: absolute;
    z-index: -1;
    padding: 1.5px;
    border-radius: inherit;
    background: conic-gradient(from 0deg, #f59e0b, #ec4899, #8b5cf6, #3b82f6, #10b981, #f59e0b);
    opacity: 0.5;
    content: '';
    inset: 0;
    mask: linear-gradient(#000000 0 0) content-box, linear-gradient(#000000 0 0);
    mask-composite: xor;
    mask: linear-gradient(#000000 0 0) content-box, linear-gradient(#000000 0 0);
    mask-composite: exclude;
    pointer-events: none;
    animation: bs-panel-beam 6s linear infinite;
  }
  .bs-chat__panel::after {
    content: '';
    position: absolute;
    inset: 1.5px;
    z-index: -1;
    border-radius: inherit;
    background: radial-gradient(36% 28% at 50% 50%, rgb(255 255 255 / 12%), transparent 72%),
      radial-gradient(28% 22% at 50% 50%, rgb(139 92 246 / 10%), transparent 72%),
      radial-gradient(32% 26% at 50% 50%, rgb(59 130 246 / 8%), transparent 72%),
      radial-gradient(40% 32% at 50% 50%, rgb(255 255 255 / 8%), transparent 72%),
      linear-gradient(180deg, rgb(255 255 255 / 6%) 0%, transparent 38%);
    background-repeat: no-repeat;
    background-size: 170% 170%, 170% 170%, 170% 170%, 170% 170%, 100% 100%;
    pointer-events: none;
    animation: bs-panel-ripple 14s ease-in-out infinite;
  }
  @keyframes bs-panel-ripple {
    0% {
      background-position: 0% 25%, 100% 75%, 30% 0%, 70% 100%, 0 0;
    }
    50% {
      background-position: 100% 75%, 0% 25%, 70% 100%, 30% 0%, 0 0;
    }
    100% {
      background-position: 0% 25%, 100% 75%, 30% 0%, 70% 100%, 0 0;
    }
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
  }
  .bs-chat__ask input {
    padding: 0 50px 0 18px;
    width: 100%;
    height: 42px;
    border: 1px solid rgb(232 232 226 / 20%);
    border-radius: 999px;
    color: #ecece9;
    background: rgb(255 255 255 / 6%);
    outline: none;
  }
  .bs-chat__ask input:focus {
    border-color: rgb(230 207 152 / 50%);
    box-shadow: 0 0 0 3px rgb(230 207 152 / 10%);
  }
  .bs-chat__ask input::placeholder {
    color: #75766f;
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
