<template>
  <n-modal
    v-model:show="show"
    preset="card"
    size="large"
    :title="t('agreement.title')"
    :bordered="false"
    class="agreement-modal"
  >
    <n-tabs v-model:value="activeKey" type="line">
      <n-tab-pane v-for="doc in agreementDocs" :key="doc.key" :name="doc.key" :tab="doc.title">
        <n-scrollbar style="max-height: 56vh">
          <div class="px-2 pb-2">
            <section v-for="section in doc.sections" :key="section.title" class="mb-4">
              <h4 class="mb-2 text-[15px] font-semibold">{{ section.title }}</h4>
              <p
                v-for="(item, idx) in section.items"
                :key="idx"
                class="mb-2 text-[13px] leading-6"
                :class="item.bold ? 'font-semibold' : 'text-[var(--text-n2)]'"
              >
                {{ item.text }}
              </p>
            </section>
          </div>
        </n-scrollbar>
      </n-tab-pane>
    </n-tabs>
  </n-modal>
</template>

<script setup lang="ts">
  import { NModal, NScrollbar, NTabPane, NTabs } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { agreementDocs } from './agreementContent';

  const { t } = useI18n();

  const show = defineModel<boolean>('show', { required: true, default: false });
  const activeKey = defineModel<'service' | 'ip' | 'privacy'>('activeKey', { default: 'service' });
</script>

<style scoped>
  .agreement-modal :deep(.n-card-header) {
    font-weight: 600;
  }
</style>
