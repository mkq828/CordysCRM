<template>
  <div class="flex flex-col gap-[12px]">
    <div class="kb-answer-card">
      <div class="flex items-center justify-between">
        <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('knowledge.answer') }}</span>
        <n-button size="tiny" quaternary @click="copy(answer)">{{ t('knowledge.copy') }}</n-button>
      </div>
      <div class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">{{ answer }}</div>
    </div>

    <template v-if="citations?.length">
      <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('knowledge.citations') }}</span>
      <div v-for="(cit, index) in citations" :key="index" class="kb-citation-card">
        <div class="flex items-center justify-between">
          <span class="truncate text-[13px] font-semibold text-[var(--text-n1)]">{{ cit.docName || '-' }}</span>
          <n-button size="tiny" quaternary @click="copy(cit.content)">{{ t('knowledge.copy') }}</n-button>
        </div>
        <div class="whitespace-pre-wrap text-[12px] leading-[1.6] text-[var(--text-n3)]">{{ cit.content }}</div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
  import { NButton, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiKnowledgeCitation } from '@lib/shared/models/ai';

  defineProps<{
    answer: string;
    citations?: AiKnowledgeCitation[];
  }>();

  const { t } = useI18n();
  const Message = useMessage();

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
