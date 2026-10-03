<template>
  <div class="border-beam" :style="{ padding }">
    <slot />
  </div>
</template>

<script setup lang="ts">
  withDefaults(
    defineProps<{
      /** 内边距，紧凑场景（加载提示条等）可传小值，如 '12px' */
      padding?: string;
    }>(),
    { padding: '20px' }
  );
</script>

<style scoped lang="less">
  .border-beam {
    position: relative;
    overflow: hidden;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    background: #ffffff;
  }
  .border-beam::before {
    content: '';
    position: absolute;
    top: 0;
    left: -150%;
    width: 60%;
    height: 100%;
    background: linear-gradient(100deg, transparent, rgb(139 92 246 / 35%), transparent);
    transform: skewX(-20deg);
    animation: border-beam-sweep 3s ease-in-out infinite;
  }

  @keyframes border-beam-sweep {
    0% {
      left: -150%;
    }
    60%,
    100% {
      left: 150%;
    }
  }
</style>
