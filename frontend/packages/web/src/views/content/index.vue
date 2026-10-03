<template>
  <div class="content-page flex h-full flex-col overflow-hidden p-[16px]">
    <CrmCard hide-footer no-content-padding content-height="100%" class="content-card min-h-0 flex-1 overflow-hidden">
      <template #title>
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
          <span class="text-[14px] font-semibold">{{ t('menu.aiContent') }}</span>
        </div>
      </template>

      <CrmSplitPanel class="h-full" :max="0.5" :min="0.2" :default-size="0.24">
        <template #1>
          <AiConversationPanel
            :feature-code="FEATURE_ACQUIRE"
            :active-id="activeConversationId"
            @select="handleSelectHistory"
            @new="handleNewConversation"
          />
        </template>

        <template #2>
          <div class="flex h-full flex-col overflow-y-auto px-[24px] pb-[24px]">
            <div class="text-[13px] text-orange-500">{{ t('content.desc') }}</div>

            <div class="mt-[16px] flex flex-col gap-[16px]">
              <div class="grid grid-cols-1 gap-[16px] md:grid-cols-2">
                <div class="flex items-center gap-[12px]">
                  <span class="w-[72px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('content.industry') }}</span>
                  <n-input v-model:value="industry" :placeholder="t('content.industryPlaceholder')" />
                </div>
                <div class="flex items-center gap-[12px]">
                  <span class="w-[72px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('content.platform') }}</span>
                  <n-select v-model:value="platform" :options="platformOptions" class="!w-[220px]" />
                </div>
              </div>

              <div class="flex items-start gap-[12px]">
                <span class="mt-[8px] w-[72px] shrink-0 text-[13px] text-[var(--text-n2)]">
                  {{ t('content.product') }}
                </span>
                <n-input
                  v-model:value="product"
                  type="textarea"
                  :placeholder="t('content.productPlaceholder')"
                  :autosize="{ minRows: 3, maxRows: 6 }"
                />
              </div>

              <div class="flex items-center gap-[12px]">
                <span class="w-[72px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('content.topicCount') }}</span>
                <n-input-number v-model:value="topicCount" :min="1" :max="10" class="!w-[120px]" />
                <n-button type="primary" :loading="generating" :disabled="!canGenerate" @click="handleGenerate">
                  {{ generating ? t('content.generating') : t('content.generate') }}
                </n-button>
              </div>
            </div>

            <div class="mt-[20px] flex flex-col gap-[16px]">
              <n-empty v-if="!thread.length && !generating" :description="t('content.empty')" />

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
                  <template v-else>
                    <div
                      v-if="item.content"
                      class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]"
                    >
                      {{ item.content }}
                    </div>
                    <ContentResult :contents="item.contents || []" :raw-content="item.rawContent" />
                  </template>
                </div>
              </template>

              <div v-if="generating" class="flex flex-col gap-[8px] rounded-[6px] bg-[var(--fill-2)] p-[12px]">
                <div class="flex items-center gap-[8px] text-[13px] text-[var(--text-n3)]">
                  <CrmIcon type="iconicon_loading" :size="16" class="animate-spin" />
                  {{ t('content.generating') }}
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
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { NButton, NEmpty, NInput, NInputNumber, NSelect } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AgentConversationDetail,
    AgentConversationMessage,
    AiContentGenerateResult,
    AiContentItem,
  } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import AiConversationPanel from '@/components/business/ai-conversation-panel/index.vue';
  import ContentResult from './components/ContentResult.vue';

  import { streamGenerateAiContent } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  const FEATURE_ACQUIRE = 'ai_acquire';

  const { t } = useI18n();
  const userStore = useUserStore();

  const industry = ref('');
  const product = ref('');
  const platform = ref<'douyin' | 'xiaohongshu' | 'moments'>('douyin');
  const topicCount = ref(5);
  const generating = ref(false);
  const streamingText = ref('');

  interface ThreadItem {
    id: string;
    role: 'USER' | 'ASSISTANT';
    content: string;
    time?: number;
    userName?: string;
    contents?: AiContentItem[];
    rawContent?: string;
    status?: string;
  }

  const thread = ref<ThreadItem[]>([]);
  const activeConversationId = ref('');

  const platformOptions = computed(() => [
    { label: t('content.platformDouyin'), value: 'douyin' },
    { label: t('content.platformXiaohongshu'), value: 'xiaohongshu' },
    { label: t('content.platformMoments'), value: 'moments' },
  ]);

  const canGenerate = computed(() => !generating.value && industry.value.trim() !== '' && product.value.trim() !== '');
  const currentUserName = computed(() => userStore.userInfo.name || '');

  function formatTime(ts?: number): string {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '';
  }

  function parsePayload(payload?: string | null): AiContentGenerateResult | null {
    if (!payload) {
      return null;
    }
    try {
      return JSON.parse(payload) as AiContentGenerateResult;
    } catch {
      return null;
    }
  }

  function toThreadItems(messages: AgentConversationMessage[]): ThreadItem[] {
    return (messages || []).map((msg) => {
      const parsed = msg.role === 'ASSISTANT' ? parsePayload(msg.payload) : null;
      return {
        id: msg.id,
        role: msg.role,
        content: msg.content || '',
        time: msg.createTime,
        userName: msg.role === 'USER' ? currentUserName.value : undefined,
        contents: parsed?.contents || [],
        rawContent: parsed?.rawContent,
        status: msg.status,
      };
    });
  }

  function handleSelectHistory(detail: AgentConversationDetail): void {
    activeConversationId.value = detail?.conversation?.id || '';
    thread.value = toThreadItems(detail?.messages || []);
  }

  function handleNewConversation(): void {
    activeConversationId.value = '';
    thread.value = [];
  }

  async function handleGenerate() {
    if (!canGenerate.value) {
      return;
    }
    generating.value = true;
    streamingText.value = '';

    const userText = `${t('content.industry')}：${industry.value.trim()}；${t(
      'content.product'
    )}：${product.value.trim()}`;
    thread.value.push({
      id: `u_${Date.now()}`,
      role: 'USER',
      content: userText,
      time: Date.now(),
      userName: currentUserName.value,
    });

    let assistantPushed = false;
    try {
      const stream = streamGenerateAiContent({
        industry: industry.value.trim(),
        product: product.value.trim(),
        platform: platform.value,
        topicCount: topicCount.value,
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
          const result = (event.data?.payload as AiContentGenerateResult) || null;
          thread.value.push({
            id: `a_${Date.now()}`,
            role: 'ASSISTANT',
            content: streamingText.value,
            time: Date.now(),
            contents: result?.contents || [],
            rawContent: result?.rawContent,
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
      generating.value = false;
      streamingText.value = '';
    }
  }
</script>

<style lang="less" scoped>
  .content-page {
    background: #f5f7fa;
  }
</style>
