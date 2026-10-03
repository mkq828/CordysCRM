<template>
  <div class="ring-gauge" :style="ringStyle">
    <div class="ring-gauge__inner">
      <slot>
        <div class="ring-gauge__value">{{ value }}</div>
        <div v-if="label" class="ring-gauge__label">{{ label }}</div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  const props = withDefaults(
    defineProps<{
      value: number;
      max?: number;
      size?: number;
      color?: string;
      label?: string;
    }>(),
    { max: 100, size: 120, color: '#ff6b6b', label: '' }
  );

  const ringStyle = computed(() => {
    const pct = props.max > 0 ? Math.min(100, Math.max(0, (props.value / props.max) * 100)) : 0;
    return {
      width: `${props.size}px`,
      height: `${props.size}px`,
      background: `conic-gradient(${props.color} 0% ${pct}%, rgb(255 255 255 / 10%) ${pct}% 100%)`,
    };
  });
</script>

<style scoped lang="less">
  .ring-gauge {
    display: flex;
    justify-content: center;
    align-items: center;
    border-radius: 50%;
  }
  .ring-gauge__inner {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 76%;
    height: 76%;
    border-radius: 50%;
    background: #131f35;
    flex-direction: column;
  }
  .ring-gauge__value {
    font-size: 24px;
    font-weight: 700;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
  }
  .ring-gauge__label {
    margin-top: 2px;
    font-size: 12px;
    color: #a0a8b8;
  }
</style>
