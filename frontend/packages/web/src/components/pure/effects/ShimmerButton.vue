<template>
  <button class="shimmer-btn" :class="{ 'shimmer-btn--block': block }" type="button">
    <span class="shimmer-btn__label"><slot /></span>
  </button>
</template>

<script setup lang="ts">
  withDefaults(
    defineProps<{
      /** 是否占满整行 */
      block?: boolean;
    }>(),
    { block: false }
  );
</script>

<style scoped lang="less">
  .shimmer-btn {
    position: relative;
    overflow: hidden;
    padding: 10px 24px;
    font-size: 14px;
    font-weight: 600;
    border: none;
    border-radius: 8px;
    color: #ffffff;
    background: linear-gradient(120deg, #f97316, #ec4899 40%, #8b5cf6);
    background-size: 220% 100%;
    box-shadow: 0 4px 14px rgb(139 92 246 / 35%);
    transition: transform 160ms cubic-bezier(0.23, 1, 0.32, 1), box-shadow 160ms cubic-bezier(0.23, 1, 0.32, 1);
    cursor: pointer;
    animation: shimmer-btn-move 3s linear infinite;
  }
  .shimmer-btn__label {
    position: relative;
    z-index: 1;
  }
  .shimmer-btn::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(100deg, transparent 20%, rgb(255 255 255 / 35%) 50%, transparent 80%);
    transform: translateX(-100%);
    animation: shimmer-btn-sheen 2.4s ease-in-out infinite;
  }
  .shimmer-btn--block {
    width: 100%;
  }

  @media (hover: hover) and (pointer: fine) {
    .shimmer-btn:hover {
      box-shadow: 0 6px 20px rgb(139 92 246 / 50%);
    }
  }
  .shimmer-btn:active {
    transform: scale(0.97);
  }

  @keyframes shimmer-btn-move {
    0% {
      background-position: 0% 50%;
    }
    50% {
      background-position: 100% 50%;
    }
    100% {
      background-position: 0% 50%;
    }
  }
  @keyframes shimmer-btn-sheen {
    0%,
    60% {
      transform: translateX(-100%);
    }
    100% {
      transform: translateX(100%);
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .shimmer-btn,
    .shimmer-btn::after {
      animation: none;
    }
  }
</style>
