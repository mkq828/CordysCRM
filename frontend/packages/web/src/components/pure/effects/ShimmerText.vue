<template>
  <span class="shimmer-text" :style="{ fontSize: size }">
    <span class="shimmer-text__base"><slot /></span>
    <span
      class="shimmer-text__shine"
      aria-hidden="true"
      :style="{
        backgroundImage: `linear-gradient(100deg, transparent 40%, ${highlight} 50%, transparent 60%)`,
      }"
    >
      <slot />
    </span>
  </span>
</template>

<script setup lang="ts">
  withDefaults(
    defineProps<{
      /** 字号，例如 28px */
      size?: string;
      /** 扫过文字的高光颜色 */
      highlight?: string;
    }>(),
    { size: '32px', highlight: 'rgb(255 255 255 / 90%)' }
  );
</script>

<style scoped lang="less">
  .shimmer-text {
    display: inline-grid;
    font-weight: 700;
  }
  .shimmer-text__base,
  .shimmer-text__shine {
    grid-area: 1 / 1;
  }

  /* 底色复用 C1 渐变流光的配色，让文字本身是彩色渐变 */
  .shimmer-text__base {
    color: transparent;
    background: linear-gradient(90deg, #f97316, #ec4899, #8b5cf6, #22d3ee, #f97316);
    background-size: 300% 100%;
    background-clip: text;
    animation: shimmer-text-base-move 4s linear infinite;
  }

  /* 高光带在彩色文字上扫过，呈镜面反光 */
  .shimmer-text__shine {
    color: transparent;
    background-repeat: no-repeat;
    background-size: 200% 100%;
    background-clip: text;
    animation: shimmer-text-sweep 2.6s ease-in-out infinite;
  }

  @keyframes shimmer-text-base-move {
    to {
      background-position: 300% 0;
    }
  }

  @keyframes shimmer-text-sweep {
    0% {
      background-position: 200% 0;
    }
    100% {
      background-position: -200% 0;
    }
  }
</style>
