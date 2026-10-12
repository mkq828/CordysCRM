<template>
  <div class="lead-crawl-page flex h-full flex-col overflow-hidden p-[16px]">
    <CrmCard
      hide-footer
      no-content-padding
      content-height="100%"
      class="lead-crawl-card min-h-0 flex-1 overflow-hidden"
    >
      <template #title>
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
          <span class="text-[14px] font-semibold">{{ t('menu.aiLeadCrawl') }}</span>
        </div>
      </template>

      <CrmSplitPanel class="h-full" :max="0.5" :min="0.2" :default-size="0.24">
        <template #1>
          <AiConversationPanel
            ref="conversationPanelRef"
            :feature-code="FEATURE_LEAD_CRAWL"
            :active-id="activeConversationId"
            :generating-id="generating ? activeConversationId : ''"
            @select="handleSelectHistory"
            @new="handleNewConversation"
          />
        </template>

        <template #2>
          <div class="flex h-full flex-col overflow-y-auto px-[24px] pb-[24px]">
            <div class="text-[13px] text-orange-500">{{ t('leadCrawl.insightDesc') }}</div>

            <div class="mt-[16px] flex flex-col gap-[16px]">
              <div class="grid grid-cols-1 gap-[16px] md:grid-cols-2">
                <div class="flex items-center gap-[12px]">
                  <span class="w-[72px] shrink-0 text-[13px] text-[var(--text-n2)]">
                    {{ t('leadCrawl.competitor') }}
                  </span>
                  <n-input v-model:value="competitor" :placeholder="t('leadCrawl.competitorPlaceholder')" />
                </div>
                <div class="flex items-center gap-[12px]">
                  <span class="w-[72px] shrink-0 text-[13px] text-[var(--text-n2)]">
                    {{ t('leadCrawl.industry') }}
                  </span>
                  <n-input v-model:value="industry" :placeholder="t('leadCrawl.industryPlaceholder')" />
                </div>
              </div>

              <div class="flex items-start gap-[12px]">
                <span class="mt-[8px] w-[72px] shrink-0 text-[13px] text-[var(--text-n2)]">
                  {{ t('leadCrawl.focus') }}
                </span>
                <n-input
                  v-model:value="focus"
                  type="textarea"
                  :placeholder="t('leadCrawl.focusPlaceholder')"
                  :autosize="{ minRows: 2, maxRows: 4 }"
                />
              </div>

              <div class="flex items-center gap-[12px]">
                <AiActionButton :loading="generating" :disabled="!canGenerate" @click="handleGenerate">
                  {{ generating ? t('leadCrawl.generating') : t('leadCrawl.generate') }}
                </AiActionButton>
              </div>
            </div>

            <div class="mt-[20px] flex flex-col gap-[16px]">
              <n-empty
                v-if="!thread.length && !generating"
                :description="t('leadCrawl.insightEmpty')"
                class="mt-[20px]"
              />

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
                    <InsightResult :insight="item.insight" />
                  </template>
                </div>
              </template>

              <div v-if="generating" class="flex flex-col gap-[8px]">
                <div class="flex items-center gap-[8px]">
                  <CrmIcon
                    type="iconicon_loading"
                    :size="16"
                    color="linear-gradient(90deg, #f97316, #ec4899, #8b5cf6, #22d3ee)"
                    class="ai-loading-spin"
                  />
                  <ShimmerText size="16px">{{ t('leadCrawl.generating') }}</ShimmerText>
                </div>
                <div class="text-[12px] text-orange-500">{{ t('leadCrawl.generatingTip') }}</div>
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
  import { NEmpty, NInput } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AgentConversationDetail,
    AgentConversationMessage,
    CompetitorInsightResult,
  } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';
  import AiConversationPanel from '@/components/business/ai-conversation-panel/index.vue';
  import InsightResult from './components/InsightResult.vue';

  import { streamCompetitorInsight } from '@/api/modules';
  import useTypewriter from '@/hooks/useTypewriter';
  import useUserStore from '@/store/modules/user';

  const FEATURE_LEAD_CRAWL = 'lead_crawl';

  const { t } = useI18n();
  const userStore = useUserStore();

  const competitor = ref('');
  const industry = ref('');
  const focus = ref('');
  const generating = ref(false);
  const { text: streamingText, feed: feedStreaming, finish: finishStreaming, reset: resetStreaming } = useTypewriter();

  interface ThreadItem {
    id: string;
    role: 'USER' | 'ASSISTANT';
    content: string;
    time?: number;
    userName?: string;
    insight?: CompetitorInsightResult;
    status?: string;
  }

  const thread = ref<ThreadItem[]>([]);
  const activeConversationId = ref('');
  const conversationPanelRef = ref<InstanceType<typeof AiConversationPanel>>();

  const canGenerate = computed(
    () => !generating.value && (competitor.value.trim() !== '' || industry.value.trim() !== '')
  );
  const currentUserName = computed(() => userStore.userInfo.name || '');

  function formatTime(ts?: number): string {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '';
  }

  function parsePayload(payload?: string | null): CompetitorInsightResult | null {
    if (!payload) {
      return null;
    }
    try {
      return JSON.parse(payload) as CompetitorInsightResult;
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
        insight: parsed || undefined,
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
    resetStreaming();

    const parts = [
      `${t('leadCrawl.competitor')}：${competitor.value.trim()}`,
      `${t('leadCrawl.industry')}：${industry.value.trim()}`,
    ];
    if (focus.value.trim()) {
      parts.push(`${t('leadCrawl.focus')}：${focus.value.trim()}`);
    }
    thread.value.push({
      id: `u_${Date.now()}`,
      role: 'USER',
      content: parts.join('；'),
      time: Date.now(),
      userName: currentUserName.value,
    });

    let assistantPushed = false;
    try {
      const stream = streamCompetitorInsight({
        competitor: competitor.value.trim(),
        industry: industry.value.trim(),
        focus: focus.value.trim(),
        conversationId: activeConversationId.value || undefined,
      });

      // eslint-disable-next-line no-restricted-syntax -- 原生 async generator，无需 regenerator-runtime
      for await (const event of stream) {
        if (event.type === 'run') {
          const conversationId = event.run?.conversationId || '';
          if (conversationId) {
            activeConversationId.value = conversationId;
            conversationPanelRef.value?.reload();
          }
        } else if (event.type === 'chunk') {
          feedStreaming(event.content || '');
        } else if (event.type === 'error') {
          throw new Error(event.errorMessage || t('common.operationFailed'));
        } else if (event.type === 'done' && !assistantPushed) {
          await finishStreaming();
          const result = (event.data?.payload as CompetitorInsightResult) || null;
          thread.value.push({
            id: `a_${Date.now()}`,
            role: 'ASSISTANT',
            content: streamingText.value,
            time: Date.now(),
            insight: result || undefined,
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
      resetStreaming();
    }
  }
</script>

<style lang="less" scoped>
  .lead-crawl-page {
    background: #f5f7fa;
  }
</style>
