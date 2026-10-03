<template>
  <div class="ai-chat-block flex items-center gap-[8px] py-[4px]">
    <CrmIcon
      type="iconicon_loading"
      :size="16"
      color="linear-gradient(90deg, #f97316, #ec4899, #8b5cf6, #22d3ee)"
      class="ai-loading-spin"
    />
    <ShimmerText size="16px">{{ t('aiChat.thinkingInProgress') }}</ShimmerText>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';

  const { t } = useI18n();
</script>

<style scoped lang="less"></style>
