<template>
  <nav class="drama-stepper" aria-label="drama workflow steps">
    <template v-for="(s, i) in steps" :key="s.key">
      <div class="drama-step" :class="{ done: i < current, active: i === current }">
        <span class="drama-step-dot">{{ i < current ? '✓' : i + 1 }}</span>
        <span>{{ s.label }}</span>
      </div>
      <div v-if="i < steps.length - 1" class="drama-step-line" :class="{ done: i < current }" />
    </template>
  </nav>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  withDefaults(defineProps<{ current?: number }>(), { current: 0 });

  const { t } = useI18n();

  const steps = computed(() => [
    { key: 'topic', label: t('drama.step.topic') },
    { key: 'style', label: t('drama.step.style') },
    { key: 'script', label: t('drama.step.script') },
    { key: 'assets', label: t('drama.step.assets') },
    { key: 'fragments', label: t('drama.step.fragments') },
    { key: 'imageVoice', label: t('drama.step.imageVoice') },
    { key: 'video', label: t('drama.step.video') },
    { key: 'compose', label: t('drama.step.compose') },
  ]);
</script>

<style scoped>
  .drama-stepper {
    display: flex;
    align-items: center;
    gap: 0.35rem;
    flex-wrap: wrap;
  }
  .drama-step {
    display: inline-flex;
    align-items: center;
    font-size: 0.82rem;
    font-weight: 500;
    white-space: nowrap;
    color: #8a94a6;
    gap: 0.4rem;
  }
  .drama-step-dot {
    display: grid;
    width: 24px;
    height: 24px;
    font-size: 0.72rem;
    font-weight: 700;
    border: 2px solid #d1d5db;
    border-radius: 999px;
    background: #ffffff;
    place-items: center;
  }
  .drama-step.done {
    color: #1f2329;
  }
  .drama-step.done .drama-step-dot {
    border-color: #b6ff00;
    color: #111318;
    background: #b6ff00;
  }
  .drama-step.active {
    font-weight: 700;
    color: #1f2329;
  }
  .drama-step.active .drama-step-dot {
    border-color: #b6ff00;
    color: #111318;
    background: #b6ff00;
  }
  .drama-step-line {
    margin: 0 0.15rem;
    width: 24px;
    height: 2px;
    background: #e5e7eb;
  }
  .drama-step-line.done {
    background: #b6ff00;
  }
</style>
