<template>
  <div class="knowledge-page flex h-full flex-col overflow-hidden p-[16px]">
    <n-tabs v-model:value="activeTab" type="line" class="knowledge-tabs min-h-0 flex-1">
      <n-tab-pane name="docs" :tab="t('knowledge.tabDocs')" class="h-full">
        <doc-list />
      </n-tab-pane>
      <n-tab-pane name="ask" :tab="t('knowledge.tabAsk')" class="h-full">
        <kb-ask />
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

<script setup lang="ts">
  import { NTabPane, NTabs } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import docList from './components/doc-list.vue';
  import kbAsk from './components/kb-ask.vue';

  const { t } = useI18n();

  const activeTab = ref('docs');
</script>

<style lang="less" scoped>
  .knowledge-page {
    background: #f5f7fa;
  }
</style>
