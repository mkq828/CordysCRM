<template>
  <CrmCard no-content-padding hide-footer>
    <div class="flex h-full items-center justify-center">
      <n-empty size="large" :description="t('dashboard.comingSoon')" />
    </div>
  </CrmCard>
</template>

<script setup lang="ts">
  import { NEmpty } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';

  // TODO: 仪表盘暂为占位页，后续替换为租户经营大屏；DataEase 看板相关代码（link.vue / module.vue）暂保留未删除。
  const { t } = useI18n();
</script>
