<template>
  <span class="pulsing-badge" :style="badgeStyle">
    <slot />
  </span>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  const props = withDefaults(
    defineProps<{
      color?: string;
      size?: number;
    }>(),
    { color: '#ff6b6b', size: 48 }
  );

  const badgeStyle = computed(() => ({
    'width': `${props.size}px`,
    'height': `${props.size}px`,
    'color': props.color,
    '--pulse-color': props.color,
  }));
</script>

<style scoped lang="less">
  .pulsing-badge {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    border-radius: 50%;
    background: rgb(255 255 255 / 8%);
  }
  .pulsing-badge::after {
    content: '';
    position: absolute;
    inset: 0;
    border: 2px solid var(--pulse-color);
    border-radius: 50%;
    animation: pulsing-badge-ring 2s ease-out infinite;
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
  @media (prefers-reduced-motion: reduce) {
    .pulsing-badge::after {
      animation: none;
    }
  }
</style>
