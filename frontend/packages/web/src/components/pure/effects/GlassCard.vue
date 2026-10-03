<template>
  <div class="glass-card" :class="{ 'glass-card--dark': dark }">
    <slot />
  </div>
</template>

<script setup lang="ts">
  withDefaults(
    defineProps<{
      /** 深色科技风（暗色底 + 微光边框），适合大屏/数据页 */
      dark?: boolean;
    }>(),
    { dark: false }
  );
</script>

<style scoped lang="less">
  .glass-card {
    position: relative;
    overflow: hidden;
    padding: 20px;
    border: 1px solid rgb(255 255 255 / 55%);
    border-radius: 16px;
    background: linear-gradient(135deg, rgb(255 255 255 / 55%) 0%, rgb(255 255 255 / 12%) 100%);
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 80%), inset 0 -1px 0 rgb(255 255 255 / 10%),
      0 8px 32px rgb(31 38 135 / 20%);
    backdrop-filter: blur(16px) saturate(160%);
    transition: border-color 200ms cubic-bezier(0.23, 1, 0.32, 1), box-shadow 200ms cubic-bezier(0.23, 1, 0.32, 1);
  }

  /* 顶部高光 + 斜向反光，模拟玻璃受光 */
  .glass-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: -20%;
    width: 60%;
    height: 100%;
    background: linear-gradient(105deg, transparent 0%, rgb(255 255 255 / 28%) 50%, transparent 100%);
    transform: skewX(-18deg);
    pointer-events: none;
  }
  .glass-card--dark {
    border-color: rgb(255 255 255 / 18%);
    color: #ffffff;
    background: linear-gradient(135deg, rgb(255 255 255 / 14%) 0%, rgb(255 255 255 / 3%) 100%);
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 18%), 0 8px 32px rgb(0 0 0 / 40%);
  }
  .glass-card--dark::before {
    background: linear-gradient(105deg, transparent 0%, rgb(255 255 255 / 10%) 50%, transparent 100%);
  }

  @media (hover: hover) and (pointer: fine) {
    .glass-card:hover {
      border-color: rgb(255 255 255 / 90%);
      box-shadow: inset 0 1px 0 rgb(255 255 255 / 90%), 0 12px 40px rgb(31 38 135 / 28%);
    }
    .glass-card--dark:hover {
      border-color: rgb(100 180 255 / 60%);
      box-shadow: inset 0 1px 0 rgb(255 255 255 / 22%), 0 8px 32px rgb(0 114 255 / 30%);
    }
  }
</style>
