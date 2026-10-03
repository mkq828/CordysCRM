<template>
  <span class="gradient-text" :style="{ fontSize: size }">
    <slot />
  </span>
</template>

<script setup lang="ts">
  withDefaults(
    defineProps<{
      /** 字号，例如 32px */
      size?: string;
    }>(),
    { size: '32px' }
  );
</script>

<style scoped lang="less">
  .gradient-text {
    font-weight: 700;
    color: transparent;
    background: linear-gradient(90deg, #f97316, #ec4899, #8b5cf6, #22d3ee, #f97316);
    background-size: 300% 100%;
    background-clip: text;
    animation: gradient-text-move 4s linear infinite;
  }

  @keyframes gradient-text-move {
    to {
      background-position: 300% 0;
    }
  }
</style>
