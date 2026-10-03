<template>
  <CrmCard hide-footer class="kb-ask h-full overflow-y-auto">
    <template #title>
      <div class="flex items-center gap-[8px]">
        <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
        <span class="text-[14px] font-semibold">{{ t('knowledge.tabAsk') }}</span>
      </div>
    </template>

    <div class="flex flex-col gap-[16px] px-[24px] pb-[24px]">
      <div class="text-[13px] text-orange-500">{{ t('knowledge.askDesc') }}</div>

      <n-input
        v-model:value="question"
        type="textarea"
        :placeholder="t('knowledge.questionPlaceholder')"
        :autosize="{ minRows: 4, maxRows: 10 }"
      />

      <div>
        <n-button type="primary" :loading="asking" :disabled="!canAsk" @click="handleAsk">
          {{ asking ? t('knowledge.asking') : t('knowledge.ask') }}
        </n-button>
      </div>

      <div v-if="result" class="flex flex-col gap-[12px]">
        <div class="kb-answer-card">
          <div class="flex items-center justify-between">
            <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('knowledge.answer') }}</span>
            <n-button size="tiny" quaternary @click="copy(result.answer)">{{ t('knowledge.copy') }}</n-button>
          </div>
          <div class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">{{ result.answer }}</div>
        </div>

        <template v-if="result.citations?.length">
          <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('knowledge.citations') }}</span>
          <div v-for="(cit, index) in result.citations" :key="index" class="kb-citation-card">
            <div class="flex items-center justify-between">
              <span class="truncate text-[13px] font-semibold text-[var(--text-n1)]">{{ cit.docName || '-' }}</span>
              <n-button size="tiny" quaternary @click="copy(cit.content)">{{ t('knowledge.copy') }}</n-button>
            </div>
            <div class="whitespace-pre-wrap text-[12px] leading-[1.6] text-[var(--text-n3)]">{{ cit.content }}</div>
          </div>
        </template>
      </div>
    </div>
  </CrmCard>
</template>

<script setup lang="ts">
  import { NButton, NInput, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiKnowledgeAnswerResult } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { askKnowledge } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const question = ref('');
  const asking = ref(false);
  const result = ref<AiKnowledgeAnswerResult | null>(null);

  const canAsk = computed(() => !asking.value && question.value.trim() !== '');

  async function handleAsk() {
    if (!canAsk.value) {
      return;
    }
    asking.value = true;
    try {
      result.value = await askKnowledge({ question: question.value.trim() });
    } catch (error) {
      result.value = null;
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      asking.value = false;
    }
  }

  async function copy(text?: string) {
    if (!text) {
      return;
    }
    try {
      await navigator.clipboard.writeText(text);
      Message.success(t('knowledge.copied'));
    } catch {
      Message.error(t('common.operationFailed'));
    }
  }
</script>

<style lang="less" scoped>
  .kb-answer-card {
    padding: 14px 16px;
    border: 1px solid rgb(249 115 22 / 16%);
    border-radius: 8px;
    background: rgb(249 115 22 / 5%);
  }
  .kb-citation-card {
    display: flex;
    flex-direction: column;
    padding: 10px 12px;
    border-radius: 6px;
    background: rgb(148 163 184 / 8%);
    gap: 8px;
  }
</style>
