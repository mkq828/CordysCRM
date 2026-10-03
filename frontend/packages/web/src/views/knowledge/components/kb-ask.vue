<template>
  <CrmCard hide-footer no-content-padding content-height="100%" class="kb-ask h-full overflow-hidden">
    <template #title>
      <div class="flex items-center gap-[8px]">
        <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
        <span class="text-[14px] font-semibold">{{ t('knowledge.tabAsk') }}</span>
      </div>
    </template>

    <CrmSplitPanel class="h-full" :max="0.5" :min="0.2" :default-size="0.24">
      <template #1>
        <AiConversationPanel
          :feature-code="FEATURE_KB"
          :active-id="activeConversationId"
          @select="handleSelectHistory"
          @new="handleNewConversation"
        />
      </template>

      <template #2>
        <div class="flex h-full flex-col overflow-y-auto px-[24px] pb-[24px]">
          <div class="text-[13px] text-orange-500">{{ t('knowledge.askDesc') }}</div>

          <div class="mt-[16px] flex flex-col gap-[16px]">
            <n-input
              v-model:value="question"
              type="textarea"
              :placeholder="t('knowledge.questionPlaceholder')"
              :autosize="{ minRows: 3, maxRows: 8 }"
            />

            <div>
              <n-button type="primary" :loading="asking" :disabled="!canAsk" @click="handleAsk">
                {{ asking ? t('knowledge.asking') : t('knowledge.ask') }}
              </n-button>
            </div>
          </div>

          <div class="mt-[20px] flex flex-col gap-[16px]">
            <n-empty v-if="!thread.length && !asking" :description="t('common.noData')" :show-icon="false" />

            <template v-for="item in thread" :key="item.id">
              <div v-if="item.role === 'USER'" class="flex justify-end">
                <div class="flex max-w-[80%] flex-col items-end gap-[4px]">
                  <div class="flex items-center gap-[6px] text-[11px] text-[var(--text-n4)]">
                    <span>{{ item.userName }}</span>
                    <span>{{ formatTime(item.time) }}</span>
                  </div>
                  <div
                    class="whitespace-pre-wrap rounded-[8px] rounded-tr-[2px] bg-[var(--primary-8)] px-[12px] py-[8px] text-[13px] leading-[1.6] text-white"
                  >
                    {{ item.content }}
                  </div>
                </div>
              </div>

              <div v-else class="flex flex-col gap-[12px]">
                <div class="flex items-center gap-[6px] text-[11px] text-[var(--text-n4)]">
                  <span class="font-semibold">{{ t('aiConversation.ai') }}</span>
                  <span>{{ formatTime(item.time) }}</span>
                </div>
                <div v-if="item.status === 'error'" class="text-[13px] text-[var(--error)]">
                  {{ item.content }}
                </div>
                <KbAnswerResult v-else :answer="item.content" :citations="item.citations" />
              </div>
            </template>

            <div v-if="asking" class="flex flex-col gap-[8px] rounded-[6px] bg-[var(--fill-2)] p-[12px]">
              <div class="flex items-center gap-[8px] text-[13px] text-[var(--text-n3)]">
                <CrmIcon type="iconicon_loading" :size="16" class="animate-spin" />
                {{ t('knowledge.asking') }}
              </div>
              <div v-if="streamingText" class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
                {{ streamingText }}
              </div>
            </div>
          </div>
        </div>
      </template>
    </CrmSplitPanel>
  </CrmCard>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { NButton, NEmpty, NInput } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AgentConversationDetail,
    AgentConversationMessage,
    AiKnowledgeAnswerResult,
    AiKnowledgeCitation,
  } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import AiConversationPanel from '@/components/business/ai-conversation-panel/index.vue';
  import KbAnswerResult from './KbAnswerResult.vue';

  import { streamAskKnowledge } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  const FEATURE_KB = 'ai_kb';

  const { t } = useI18n();
  const userStore = useUserStore();

  const question = ref('');
  const asking = ref(false);
  const streamingText = ref('');

  interface ThreadItem {
    id: string;
    role: 'USER' | 'ASSISTANT';
    content: string;
    time?: number;
    userName?: string;
    citations?: AiKnowledgeCitation[];
    status?: string;
  }

  const thread = ref<ThreadItem[]>([]);
  const activeConversationId = ref('');

  const canAsk = computed(() => !asking.value && question.value.trim() !== '');
  const currentUserName = computed(() => userStore.userInfo.name || '');

  function formatTime(ts?: number): string {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '';
  }

  function parsePayload(payload?: string | null): AiKnowledgeAnswerResult | null {
    if (!payload) {
      return null;
    }
    try {
      return JSON.parse(payload) as AiKnowledgeAnswerResult;
    } catch {
      return null;
    }
  }

  function toThreadItems(messages: AgentConversationMessage[]): ThreadItem[] {
    return (messages || []).map((msg) => ({
      id: msg.id,
      role: msg.role,
      content: msg.content || '',
      time: msg.createTime,
      userName: msg.role === 'USER' ? currentUserName.value : undefined,
      citations: msg.role === 'ASSISTANT' ? parsePayload(msg.payload)?.citations : undefined,
      status: msg.status,
    }));
  }

  function handleSelectHistory(detail: AgentConversationDetail): void {
    activeConversationId.value = detail?.conversation?.id || '';
    thread.value = toThreadItems(detail?.messages || []);
  }

  function handleNewConversation(): void {
    activeConversationId.value = '';
    thread.value = [];
  }

  async function handleAsk() {
    if (!canAsk.value) {
      return;
    }
    asking.value = true;
    streamingText.value = '';

    thread.value.push({
      id: `u_${Date.now()}`,
      role: 'USER',
      content: question.value.trim(),
      time: Date.now(),
      userName: currentUserName.value,
    });

    let assistantPushed = false;
    try {
      const stream = streamAskKnowledge({
        question: question.value.trim(),
        conversationId: activeConversationId.value || undefined,
      });

      // eslint-disable-next-line no-restricted-syntax -- 原生 async generator，无需 regenerator-runtime
      for await (const event of stream) {
        if (event.type === 'run') {
          const conversationId = event.run?.conversationId || '';
          if (conversationId) {
            activeConversationId.value = conversationId;
          }
        } else if (event.type === 'chunk') {
          streamingText.value += event.content || '';
        } else if (event.type === 'error') {
          throw new Error(event.errorMessage || t('common.operationFailed'));
        } else if (event.type === 'done' && !assistantPushed) {
          const result = (event.data?.payload as AiKnowledgeAnswerResult) || null;
          thread.value.push({
            id: `a_${Date.now()}`,
            role: 'ASSISTANT',
            content: streamingText.value || result?.answer || '',
            time: Date.now(),
            citations: result?.citations || [],
            status: 'done',
          });
          assistantPushed = true;
        }
      }
    } catch (error) {
      if (!assistantPushed) {
        thread.value.push({
          id: `a_${Date.now()}`,
          role: 'ASSISTANT',
          content: (error as Error)?.message || t('common.operationFailed'),
          time: Date.now(),
          status: 'error',
        });
      }
    } finally {
      asking.value = false;
      streamingText.value = '';
      question.value = '';
    }
  }
</script>

<style lang="less" scoped></style>
