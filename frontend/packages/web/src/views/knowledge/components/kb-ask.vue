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
          ref="conversationPanelRef"
          :feature-code="FEATURE_KB"
          :active-id="activeConversationId"
          :generating-id="asking ? activeConversationId : ''"
          @select="handleSelectHistory"
          @new="handleNewConversation"
        />
      </template>

      <template #2>
        <div class="flex h-full flex-col px-[24px] pb-[24px]">
          <div class="shrink-0">
            <div class="mb-[12px] flex items-center justify-end gap-[8px]">
              <span class="text-[12px] text-[var(--text-n3)]">{{ t('knowledge.snippetMaxLabel') }}</span>
              <n-input-number
                v-model:value="snippetMax"
                :min="10"
                :max="2000"
                :step="10"
                size="small"
                :show-button="false"
                class="w-[110px]"
              />
              <n-button size="small" type="primary" :loading="savingSnippet" @click="saveSnippet">
                {{ t('common.save') }}
              </n-button>
            </div>

            <div class="text-[13px] text-orange-500">{{ t('knowledge.askDesc') }}</div>

            <div class="mt-[16px] flex flex-col gap-[16px]">
              <n-input
                v-model:value="question"
                type="textarea"
                :placeholder="t('knowledge.questionPlaceholder')"
                :autosize="{ minRows: 3, maxRows: 8 }"
              />

              <div>
                <AiActionButton :loading="asking" :disabled="!canAsk" @click="handleAsk">
                  {{ asking ? t('knowledge.asking') : t('knowledge.ask') }}
                </AiActionButton>
              </div>
            </div>
          </div>

          <div ref="threadScrollRef" class="mt-[20px] flex min-h-0 flex-1 flex-col gap-[16px] overflow-y-auto">
            <n-empty v-if="!thread.length && !asking" :description="t('common.noData')" :show-icon="false" />

            <template v-for="(block, blockIndex) in displayBlocks" :key="block.id">
              <div v-if="block.question" class="flex justify-end">
                <div class="flex max-w-[80%] flex-col items-end gap-[4px]">
                  <div class="flex items-center gap-[6px] text-[11px] text-[var(--text-n4)]">
                    <span>{{ block.question.userName }}</span>
                    <span>{{ formatTime(block.question.time) }}</span>
                  </div>
                  <div
                    class="whitespace-pre-wrap rounded-[8px] rounded-tr-[2px] bg-[var(--primary-8)] px-[12px] py-[8px] text-[13px] leading-[1.6] text-white"
                  >
                    {{ block.question.content }}
                  </div>
                </div>
              </div>

              <div v-if="block.answer" class="flex flex-col gap-[12px]">
                <div class="flex items-center gap-[6px] text-[11px] text-[var(--text-n4)]">
                  <span class="font-semibold">{{ t('aiConversation.ai') }}</span>
                  <span>{{ formatTime(block.answer.time) }}</span>
                </div>
                <div v-if="block.answer.status === 'error'" class="text-[13px] text-[var(--error)]">
                  {{ block.answer.content }}
                </div>
                <KbAnswerResult v-else :answer="block.answer.content" :citations="block.answer.citations" />
              </div>

              <div v-if="blockIndex === 0 && asking && !block.answer" class="flex flex-col gap-[8px]">
                <div class="flex items-center gap-[8px]">
                  <CrmIcon
                    type="iconicon_loading"
                    :size="16"
                    color="linear-gradient(90deg, #f97316, #ec4899, #8b5cf6, #22d3ee)"
                    class="ai-loading-spin"
                  />
                  <ShimmerText size="16px">{{ t('knowledge.asking') }}</ShimmerText>
                </div>
                <div v-if="streamingText" class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
                  {{ streamingText }}
                </div>
              </div>
            </template>
          </div>
        </div>
      </template>
    </CrmSplitPanel>
  </CrmCard>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, ref, watch } from 'vue';
  import { NButton, NEmpty, NInput, NInputNumber, useMessage } from 'naive-ui';
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
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';
  import AiConversationPanel from '@/components/business/ai-conversation-panel/index.vue';
  import KbAnswerResult from './KbAnswerResult.vue';

  import { getAiKnowledgeConfig, saveAiKnowledgeConfig, streamAskKnowledge } from '@/api/modules';
  import useTypewriter from '@/hooks/useTypewriter';
  import useUserStore from '@/store/modules/user';

  const FEATURE_KB = 'ai_kb';

  const { t } = useI18n();
  const userStore = useUserStore();
  const Message = useMessage();

  const question = ref('');
  const asking = ref(false);
  const { text: streamingText, feed: feedStreaming, finish: finishStreaming, reset: resetStreaming } = useTypewriter();
  const snippetMax = ref<number | null>(200);
  const savingSnippet = ref(false);

  interface ThreadItem {
    id: string;
    role: 'USER' | 'ASSISTANT';
    content: string;
    time?: number;
    userName?: string;
    citations?: AiKnowledgeCitation[];
    status?: string;
  }

  interface ThreadBlock {
    id: string;
    question?: ThreadItem;
    answer?: ThreadItem;
  }

  const thread = ref<ThreadItem[]>([]);
  const activeConversationId = ref('');
  const conversationPanelRef = ref<InstanceType<typeof AiConversationPanel>>();
  const threadScrollRef = ref<HTMLElement | null>(null);

  const canAsk = computed(() => !asking.value && question.value.trim() !== '');
  const currentUserName = computed(() => userStore.userInfo.name || '');
  // 按「一问一答」分组：每个问题开启一个块，紧随其后的 AI 回答归入同一块
  const threadBlocks = computed<ThreadBlock[]>(() =>
    thread.value.reduce<ThreadBlock[]>((blocks, item) => {
      if (item.role === 'USER') {
        blocks.push({ id: item.id, question: item });
      } else if (blocks.length > 0 && !blocks[blocks.length - 1].answer) {
        blocks[blocks.length - 1].answer = item;
      } else {
        blocks.push({ id: item.id, answer: item });
      }
      return blocks;
    }, [])
  );

  // 最新一问排在最上，块内保持「问题在上、答案在下」
  const displayBlocks = computed<ThreadBlock[]>(() => [...threadBlocks.value].reverse());

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

  async function scrollThreadToTop(): Promise<void> {
    await nextTick();
    const el = threadScrollRef.value;
    if (el) {
      el.scrollTop = 0;
    }
  }

  async function saveSnippet() {
    if (snippetMax.value == null) {
      return;
    }
    savingSnippet.value = true;
    try {
      await saveAiKnowledgeConfig({ snippetMax: snippetMax.value });
      Message.success(t('knowledge.snippetSaved'));
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      savingSnippet.value = false;
    }
  }

  onMounted(async () => {
    try {
      const config = await getAiKnowledgeConfig();
      if (config?.snippetMax) {
        snippetMax.value = config.snippetMax;
      }
    } catch {
      // 读取失败时保留默认值，不打断问答主流程
    }
  });

  // 新消息入列时回到消息区顶部，保证最新答案一眼可见；流式逐字推进期间不强制滚动，避免与用户手动翻阅打架
  watch(
    () => thread.value.length,
    () => {
      scrollThreadToTop();
    }
  );

  async function handleAsk() {
    if (!canAsk.value) {
      return;
    }
    asking.value = true;
    resetStreaming();

    thread.value.push({
      id: `u_${Date.now()}`,
      role: 'USER',
      content: question.value.trim(),
      time: Date.now(),
      userName: currentUserName.value,
    });

    let assistantPushed = false;
    // 兜底：SSE 消费超过 60s 未收到 done，视为流中断，主动中止并在 finally 里补可见错误，避免静默失败
    const controller = new AbortController();
    const timeoutId = window.setTimeout(() => controller.abort(), 60_000);

    try {
      const stream = streamAskKnowledge(
        {
          question: question.value.trim(),
          conversationId: activeConversationId.value || undefined,
        },
        { signal: controller.signal }
      );

      // eslint-disable-next-line no-restricted-syntax -- 原生 async generator，无需 regenerator-runtime
      for await (const event of stream) {
        if (event.type === 'run') {
          const conversationId = event.run?.conversationId || '';
          if (conversationId) {
            activeConversationId.value = conversationId;
            // 生成开始后立即刷新左侧对话记录，让「正在生成」的对话实时出现在列表里
            conversationPanelRef.value?.reload();
          }
        } else if (event.type === 'chunk') {
          feedStreaming(event.content || '');
        } else if (event.type === 'error') {
          throw new Error(event.errorMessage || t('common.operationFailed'));
        } else if (event.type === 'done' && !assistantPushed) {
          await finishStreaming();
          const result = (event.data?.payload as AiKnowledgeAnswerResult) || null;
          const content = streamingText.value || result?.answer || '';
          // done 到了但无任何内容，视为异常，走 error 兜底，避免渲染空消息
          if (!content) {
            throw new Error(t('common.operationFailed'));
          }
          thread.value.push({
            id: `a_${Date.now()}`,
            role: 'ASSISTANT',
            content,
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
        assistantPushed = true;
      }
    } finally {
      window.clearTimeout(timeoutId);
      asking.value = false;
      resetStreaming();
      question.value = '';
      // 兜底：流正常结束但未渲染出任何答案（如中断/超时），补一条可见错误
      if (!assistantPushed) {
        thread.value.push({
          id: `a_${Date.now()}`,
          role: 'ASSISTANT',
          content: t('knowledge.askTimeout'),
          time: Date.now(),
          status: 'error',
        });
      }
    }
  }
</script>

<style lang="less" scoped></style>
