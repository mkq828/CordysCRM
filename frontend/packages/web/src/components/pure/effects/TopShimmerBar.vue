<template>
  <div class="top-shimmer" :style="barStyle"></div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  const props = withDefaults(
    defineProps<{
      height?: number;
      colors?: string[];
    }>(),
    { height: 4, colors: () => ['#ffa726', '#ff6b6b', '#4caf50', '#2196f3'] }
  );

  const barStyle = computed(() => ({
    height: `${props.height}px`,
    background: `linear-gradient(90deg, ${props.colors.join(', ')})`,
    backgroundSize: '400% 100%',
  }));
</script>

<style scoped lang="less">
  .top-shimmer {
    width: 100%;
    animation: top-shimmer-move 3s linear infinite;
  }

  @keyframes top-shimmer-move {
    0% {
      background-position: -400% 0;
    }
    100% {
      background-position: 400% 0;
    }
  }
  @media (prefers-reduced-motion: reduce) {
    .top-shimmer {
      animation: none;
    }
  }
</style>
