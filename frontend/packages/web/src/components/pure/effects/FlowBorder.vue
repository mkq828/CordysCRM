<template>
  <div class="flow-border" :style="{ borderRadius: `${radius}px` }">
    <div class="flow-border__inner" :style="{ borderRadius: `${Math.max(radius - 2, 0)}px` }">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
  withDefaults(
    defineProps<{
      /** 圆角大小（px） */
      radius?: number;
    }>(),
    { radius: 12 }
  );
</script>

<style scoped lang="less">
  .flow-border {
    position: relative;
    overflow: hidden;
    padding: 2px;
  }
  .flow-border::before {
    content: '';
    position: absolute;
    inset: -150%;
    background: conic-gradient(from 0deg, #f97316, #ec4899, #8b5cf6, #22d3ee, #f97316);
    animation: flow-border-spin 4s linear infinite;
  }
  .flow-border__inner {
    position: relative;
    z-index: 1;
    background: #ffffff;
  }

  @keyframes flow-border-spin {
    to {
      transform: rotate(360deg);
    }
  }
</style>
