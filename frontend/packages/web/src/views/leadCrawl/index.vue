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

      <n-tabs v-model:value="activeTab" type="line" class="lead-crawl-tabs h-full">
        <!-- 线索爬取 -->
        <n-tab-pane name="crawl" :tab="t('leadCrawl.crawlTab')" class="h-full">
          <div class="flex h-full flex-col gap-[12px] overflow-hidden p-[16px]">
            <div class="text-[13px] text-orange-500">{{ t('leadCrawl.desc') }}</div>

            <div class="flex items-center gap-[12px]">
              <n-input
                v-model:value="keyword"
                :placeholder="t('leadCrawl.keywordPlaceholder')"
                class="!w-[320px]"
                clearable
                @keyup.enter="handleSearch"
              />
              <n-button type="primary" :loading="searching" @click="handleSearch">
                {{ searching ? t('leadCrawl.searching') : t('leadCrawl.search') }}
              </n-button>
              <div class="flex-1"></div>
              <n-button :disabled="!selectedNames.length" :loading="fetching" @click="handleFetchDetail">
                {{ fetching ? t('leadCrawl.fetching') : t('leadCrawl.fetchDetail') }}
              </n-button>
              <n-button type="primary" :disabled="!selectedNames.length" :loading="importing" @click="handleImport">
                {{ importing ? t('leadCrawl.importing') : t('leadCrawl.import') }}
              </n-button>
            </div>

            <div class="min-h-0 flex-1 overflow-auto">
              <n-empty v-if="!rows.length && !searching" :description="t('leadCrawl.empty')" class="mt-[40px]" />
              <n-data-table
                v-else
                v-model:checked-row-keys="checkedRowKeys"
                :columns="columns"
                :data="rows"
                :row-key="(row: LeadCrawlEnterprise) => row.name"
                :bordered="false"
                size="small"
                :scroll-x="1300"
              />
            </div>
          </div>
        </n-tab-pane>

        <!-- 竞品洞察 -->
        <n-tab-pane name="insight" :tab="t('leadCrawl.insightTab')" class="h-full">
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
                    <div
                      v-if="streamingText"
                      class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]"
                    >
                      {{ streamingText }}
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </CrmSplitPanel>
        </n-tab-pane>
      </n-tabs>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, ref } from 'vue';
  import { NButton, NDataTable, NEmpty, NInput, NTabPane, NTabs, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AgentConversationDetail,
    AgentConversationMessage,
    CompetitorInsightResult,
    LeadCrawlEnterprise,
  } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';
  import AiConversationPanel from '@/components/business/ai-conversation-panel/index.vue';
  import InsightResult from './components/InsightResult.vue';

  import {
    fetchLeadCrawlDetail,
    importLeadCrawl,
    searchLeadCrawlEnterprise,
    streamCompetitorInsight,
  } from '@/api/modules';
  import useTypewriter from '@/hooks/useTypewriter';
  import useUserStore from '@/store/modules/user';

  import type { DataTableColumns } from 'naive-ui';

  const FEATURE_LEAD_CRAWL = 'lead_crawl';

  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  // ==================== 线索爬取 ====================
  const activeTab = ref<'crawl' | 'insight'>('crawl');
  const keyword = ref('');
  const searching = ref(false);
  const fetching = ref(false);
  const importing = ref(false);
  const rows = ref<LeadCrawlEnterprise[]>([]);
  const checkedRowKeys = ref<string[]>([]);

  const selectedNames = computed(() => checkedRowKeys.value);

  const columns = computed<DataTableColumns<LeadCrawlEnterprise>>(() => [
    { type: 'selection', width: 46 },
    { title: t('leadCrawl.colName'), key: 'name', minWidth: 200 },
    { title: t('leadCrawl.colLegalPerson'), key: 'legalPerson', width: 100, render: (row) => row.legalPerson || '-' },
    { title: t('leadCrawl.colCreditCode'), key: 'creditCode', minWidth: 160, render: (row) => row.creditCode || '-' },
    { title: t('leadCrawl.colStatus'), key: 'status', width: 90, render: (row) => row.status || '-' },
    {
      title: t('leadCrawl.colSource'),
      key: 'source',
      width: 90,
      render: (row) =>
        h(
          'span',
          {
            style: `padding:2px 8px;border-radius:4px;font-size:12px;${
              row.fromCache
                ? 'color:#f97316;background:rgba(249,115,22,0.1);'
                : 'color:#8a94a6;background:rgba(138,148,166,0.12);'
            }`,
          },
          row.fromCache ? t('leadCrawl.fromCache') : t('leadCrawl.fromQcc')
        ),
    },
    {
      title: t('leadCrawl.colArea'),
      key: 'area',
      width: 140,
      render: (row) => {
        const area = [row.areaProvince, row.areaCity].filter(Boolean).join(' ');
        return area || t('leadCrawl.notFetched');
      },
    },
    {
      title: t('leadCrawl.colIndustry'),
      key: 'industry',
      minWidth: 140,
      render: (row) => row.industry || t('leadCrawl.notFetched'),
    },
    { title: t('leadCrawl.colPhone'), key: 'registerPhone', width: 120, render: (row) => row.registerPhone || '-' },
    { title: t('leadCrawl.colCapi'), key: 'registerCapi', width: 120, render: (row) => row.registerCapi || '-' },
  ]);

  async function handleSearch() {
    const kw = keyword.value.trim();
    if (!kw) {
      Message.warning(t('leadCrawl.keywordPlaceholder'));
      return;
    }
    searching.value = true;
    checkedRowKeys.value = [];
    try {
      rows.value = await searchLeadCrawlEnterprise({ keyword: kw });
      if (!rows.value.length) {
        Message.info(t('leadCrawl.empty'));
      }
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      searching.value = false;
    }
  }

  async function handleFetchDetail() {
    if (!selectedNames.value.length) {
      Message.warning(t('leadCrawl.selectHint'));
      return;
    }
    fetching.value = true;
    try {
      const details = await fetchLeadCrawlDetail({ names: selectedNames.value });
      const detailMap = new Map(details.map((item) => [item.name, item]));
      rows.value = rows.value.map((row) => detailMap.get(row.name) || row);
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      fetching.value = false;
    }
  }

  async function handleImport() {
    if (!selectedNames.value.length) {
      Message.warning(t('leadCrawl.selectHint'));
      return;
    }
    importing.value = true;
    try {
      const result = await importLeadCrawl({ names: selectedNames.value });
      if (result.successCount > 0) {
        Message.success(t('leadCrawl.importSuccess', { count: result.successCount }));
      }
      if (result.failedNames && result.failedNames.length > 0) {
        Message.warning(t('leadCrawl.importFailed', { count: result.failedNames.length }));
      }
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      importing.value = false;
    }
  }

  // ==================== 竞品洞察 ====================
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
  .lead-crawl-tabs {
    :deep(.n-tabs-nav) {
      padding: 0 16px;
    }
    :deep(.n-tab-pane) {
      height: calc(100% - 44px);
    }
  }
</style>
