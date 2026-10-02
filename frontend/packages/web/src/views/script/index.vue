<template>
  <div class="script-page flex h-full flex-col overflow-hidden p-[16px]">
    <n-tabs v-model:value="activeTab" type="line" class="script-tabs min-h-0 flex-1">
      <n-tab-pane name="maintain" :tab="t('script.tabMaintain')" class="h-full">
        <script-list />
      </n-tab-pane>
      <n-tab-pane name="retrieve" :tab="t('script.tabRetrieve')" class="h-full">
        <script-retrieve />
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

<script setup lang="ts">
  import { NTabPane, NTabs } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import scriptList from './components/script-list.vue';
  import scriptRetrieve from './components/script-retrieve.vue';

  const { t } = useI18n();

  const activeTab = ref('maintain');
</script>

<style lang="less" scoped>
  .script-page {
    background: #f5f7fa;
  }
</style>
