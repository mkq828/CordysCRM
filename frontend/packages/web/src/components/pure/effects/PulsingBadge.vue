<template>
  <span class="pulsing-badge" :style="badgeStyle">
    <span class="pulsing-badge__content" :class="{ 'is-pulsing': pulseText }"><slot /></span>
  </span>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  const props = withDefaults(
    defineProps<{
      color?: string;
      size?: number;
      /** 中心文字是否随脉冲由小到大循环缩放 */
      pulseText?: boolean;
      /** 圆环线框宽度(px) */
      ringWidth?: number;
    }>(),
    { color: '#ff6b6b', size: 48, pulseText: false, ringWidth: 2 }
  );

  /** 把 #rrggbb / #rgb 解析为 "r g b" 供 rgb() 低透明度背景使用 */
  function hexToRgb(hex: string): string {
    const value = hex.replace('#', '');
    const full =
      value.length === 3
        ? value
            .split('')
            .map((c) => c + c)
            .join('')
        : value;
    return [
      Number.parseInt(full.slice(0, 2), 16),
      Number.parseInt(full.slice(2, 4), 16),
      Number.parseInt(full.slice(4, 6), 16),
    ].join(' ');
  }

  const badgeStyle = computed(() => ({
    'width': `${props.size}px`,
    'height': `${props.size}px`,
    'color': props.color,
    '--pulse-color': props.color,
    '--ring-width': `${props.ringWidth}px`,
    'background': `rgb(${hexToRgb(props.color)} / 0.1)`,
  }));
</script>

<style scoped lang="less">
  .pulsing-badge {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    border-radius: 50%;
  }
  .pulsing-badge::after {
    content: '';
    position: absolute;
    inset: 0;
    border: var(--ring-width, 2px) solid var(--pulse-color);
    border-radius: 50%;
    box-shadow: 0 0 12px var(--pulse-color);
    animation: pulsing-badge-ring 2s ease-out infinite;
  }
  .pulsing-badge__content {
    display: inline-flex;
    justify-content: center;
    align-items: center;
  }
  .pulsing-badge__content.is-pulsing {
    animation: pulsing-badge-text 1.6s ease-in-out infinite;
  }

  @keyframes pulsing-badge-ring {
    0% {
      transform: scale(1);
      opacity: 0.7;
    }
    100% {
      transform: scale(1.8);
      opacity: 0;
    }
  }

  @keyframes pulsing-badge-text {
    0%,
    100% {
      transform: scale(0.72);
    }
    50% {
      transform: scale(1.12);
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .pulsing-badge::after,
    .pulsing-badge__content.is-pulsing {
      animation: none;
    }
  }
</style>
