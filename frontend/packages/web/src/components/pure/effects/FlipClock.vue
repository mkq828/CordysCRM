<template>
  <div class="flip-clock">
    <div class="flip-clock__digits">
      <div v-for="(ch, i) in chars" :key="`${i}-${ch}`" class="flip-digit">{{ ch }}</div>
    </div>
    <div v-if="unit" class="flip-clock__unit">
      <span class="flip-clock__unit-main">{{ unit }}</span>
      <span v-if="label" class="flip-clock__unit-sub">{{ label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  const props = withDefaults(
    defineProps<{
      value: string;
      unit?: string;
      label?: string;
    }>(),
    { unit: '', label: '' }
  );

  const chars = computed(() => props.value.split(''));
</script>

<style scoped lang="less">
  .flip-clock {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .flip-clock__digits {
    display: flex;
    gap: 6px;
  }
  .flip-digit {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 52px;
    height: 70px;
    font-size: 36px;
    font-weight: 700;
    border-radius: 8px;
    color: #ffffff;
    background: linear-gradient(135deg, #2196f3, #1565c0);
    box-shadow: 0 4px 10px rgb(0 0 0 / 30%);
    animation: flip-digit-in 500ms cubic-bezier(0.23, 1, 0.32, 1);
    font-variant-numeric: tabular-nums;
  }
  .flip-clock__unit {
    display: flex;
    justify-content: center;
    align-items: center;
    margin-left: 4px;
    flex-direction: column;
  }
  .flip-clock__unit-main {
    font-size: 20px;
    color: #a0a8b8;
  }
  .flip-clock__unit-sub {
    margin-top: 5px;
    font-size: 14px;
    color: #a0a8b8;
  }

  @keyframes flip-digit-in {
    from {
      transform: rotateX(-90deg);
      opacity: 0.4;
    }
    to {
      transform: rotateX(0);
      opacity: 1;
    }
  }
  @media (prefers-reduced-motion: reduce) {
    .flip-digit {
      animation: none;
    }
  }
</style>
