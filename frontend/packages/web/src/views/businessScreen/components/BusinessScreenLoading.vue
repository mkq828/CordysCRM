<template>
  <div class="bs-loading">
    <span class="bs-loading__dot"></span>
    <span class="bs-loading__text">{{ t('businessScreen.loading') }}</span>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();
</script>

<style scoped lang="less">
  .bs-loading {
    position: fixed;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #8ea0bd;
    background: radial-gradient(ellipse 120% 90% at 50% 50%, #141515 0%, #090a0a 72%);
    inset: 0;
    flex-direction: column;
    gap: 16px;
  }
  .bs-loading__dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #4282ff;
    box-shadow: 0 0 18px #4282ff;
    animation: bs-loading-pulse 1.2s ease-in-out infinite;
  }
  .bs-loading__text {
    font-size: 14px;
    letter-spacing: 0.12em;
  }
  @keyframes bs-loading-pulse {
    50% {
      opacity: 0.35;
      transform: scale(0.7);
    }
  }
</style>
