<template>
  <div class="h-[800px]">
    <CrmCard hide-footer no-content-padding content-height="100%" class="sales-advisor h-full overflow-hidden">
      <template #header>
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
          <span class="text-[14px] font-semibold">{{ t('workbench.smart.advisorTitle') }}</span>
        </div>
      </template>

      <CrmSplitPanel class="h-full" :max="0.5" :min="0.2" :default-size="0.24">
        <template #1>
          <AiConversationPanel
            ref="conversationPanelRef"
            :feature-code="FEATURE_ADVISOR"
            :active-id="activeConversationId"
            :generating-id="analyzing ? activeConversationId : ''"
            @select="handleSelectHistory"
            @new="handleNewConversation"
          />
        </template>

        <template #2>
          <div class="flex h-full flex-col px-[24px] pb-[16px]">
            <div class="shrink-0 pt-[16px] text-[13px] text-orange-500">{{ t('workbench.smart.advisorDesc') }}</div>

            <div class="order-3 flex shrink-0 flex-col gap-[12px] border-t border-[var(--text-n9)] py-[12px]">
              <n-radio-group v-model:value="queryMode" size="small">
                <n-radio-button value="analyze">{{ t('workbench.smart.advisorModeAnalyze') }}</n-radio-button>
                <n-radio-button value="query">{{ t('workbench.smart.advisorModeQuery') }}</n-radio-button>
              </n-radio-group>

              <n-select
                v-if="queryMode === 'analyze'"
                v-model:value="customerId"
                clearable
                filterable
                remote
                :loading="customerLoading"
                :placeholder="t('workbench.smart.advisorCustomerPlaceholder')"
                :options="customerOptions"
                @search="handleSearchCustomer"
              />

              <div v-if="queryMode === 'analyze'" class="flex items-center gap-[8px]">
                <n-switch v-model:value="enableWebSearch" size="small" :disabled="analyzing" />
                <span class="text-[13px] text-orange-500">{{ t('workbench.smart.advisorWebSearch') }}</span>
              </div>

              <div v-if="queryMode === 'analyze' && pics.length" class="flex flex-wrap items-center gap-[8px]">
                <n-tag
                  v-for="(pic, index) in pics"
                  :key="pic.id"
                  size="small"
                  closable
                  :disabled="analyzing"
                  @close="removePic(index)"
                >
                  {{ pic.name }}
                </n-tag>
              </div>

              <div class="advisor-composer flex flex-col rounded-[8px] border border-[var(--text-n8)]">
                <n-input
                  v-model:value="message"
                  type="textarea"
                  :bordered="false"
                  :placeholder="
                    queryMode === 'query'
                      ? t('workbench.smart.advisorQueryPlaceholder')
                      : t('workbench.smart.advisorPlaceholder')
                  "
                  :autosize="{ minRows: 3, maxRows: 10 }"
                />

                <div class="flex items-center justify-between border-t border-[var(--text-n8)] px-[8px] py-[6px]">
                  <n-upload
                    v-if="queryMode === 'analyze'"
                    :custom-request="customUpload"
                    :show-file-list="false"
                    accept="image/*"
                    multiple
                  >
                    <n-button
                      text
                      class="advisor-tool-button"
                      :title="t('workbench.smart.advisorUploadTip')"
                      :disabled="analyzing"
                    >
                      <template #icon>
                        <CrmIcon type="iconicon_link1" :size="16" color="var(--text-n4)" />
                      </template>
                    </n-button>
                  </n-upload>
                  <span v-else />

                  <n-button
                    circle
                    size="small"
                    type="primary"
                    :loading="analyzing"
                    :disabled="!canAnalyze"
                    @click="handleAnalyze"
                  >
                    <template #icon>
                      <CrmIcon type="iconicon_send" :size="14" color="var(--text-n10)" />
                    </template>
                  </n-button>
                </div>
              </div>
            </div>

            <div
              ref="scrollContainerRef"
              class="order-2 mt-[12px] flex flex-1 flex-col gap-[16px] overflow-y-auto rounded-[8px] border border-[var(--text-n8)] p-[16px]"
            >
              <n-empty
                v-if="!thread.length && !streaming"
                :description="t('workbench.smart.advisorNoResult')"
                :show-icon="false"
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
                    <template v-if="item.queryResult?.queried && item.queryResult.rows?.length">
                      <n-collapse>
                        <n-collapse-item
                          :title="`${t('workbench.smart.advisorDataSource')}（${item.queryResult.rows.length} 条）`"
                          name="data"
                        >
                          <n-data-table
                            :columns="toQueryColumns(item.queryResult.columns)"
                            :data="toQueryRows(item.queryResult)"
                            size="small"
                            :max-height="320"
                          />
                        </n-collapse-item>
                      </n-collapse>
                    </template>
                    <AdvisorResult v-if="item.payload" :result="item.payload" />
                    <div v-if="item.payload" class="flex justify-end">
                      <n-button size="small" type="primary" @click="handleToFollow(item.payload)">
                        {{ t('workbench.smart.advisorToFollow') }}
                      </n-button>
                    </div>
                  </template>
                </div>
              </template>

              <div v-if="streaming" class="flex flex-col gap-[8px]">
                <div v-if="!streamingText" class="flex items-center gap-[8px]">
                  <CrmIcon
                    type="iconicon_loading"
                    :size="16"
                    color="linear-gradient(90deg, #f97316, #ec4899, #8b5cf6, #22d3ee)"
                    class="ai-loading-spin"
                  />
                  <ShimmerText size="16px">{{ t('workbench.smart.advisorThinking') }}</ShimmerText>
                </div>
                <template v-else>
                  <div class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
                    {{ streamingText }}<span class="advisor-cursor"></span>
                  </div>
                  <div class="flex items-center gap-[6px] text-[12px] text-[var(--text-n4)]">
                    <CrmIcon type="iconicon_loading" :size="12" color="var(--primary-8)" class="ai-loading-spin" />
                    <span>{{ t('workbench.smart.advisorThinking') }}</span>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </template>
      </CrmSplitPanel>

      <CrmFormCreateDrawer
        v-model:visible="followDrawerVisible"
        :form-key="followFormKey"
        :source-id="followSourceId"
        :initial-source-name="followInitialSourceName"
        :other-save-params="followOtherSaveParams"
        :link-form-key="followLinkFormKey"
        :link-scenario="followLinkScenario"
        :initial-values="followInitialValues"
        @saved="handleFollowSaved"
      />
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, ref, watch } from 'vue';
  import {
    NButton,
    NCollapse,
    NCollapseItem,
    NDataTable,
    NEmpty,
    NInput,
    NRadioButton,
    NRadioGroup,
    NSelect,
    NSwitch,
    NTag,
    NUpload,
    type UploadCustomRequestOptions,
    useMessage,
  } from 'naive-ui';
  import dayjs from 'dayjs';

  import { FormDesignKeyEnum, FormLinkScenarioEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AgentConversationDetail,
    AgentConversationMessage,
    SalesAdvisorAnalyzeResult,
    SalesAdvisorQueryResult,
  } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiConversationPanel from '@/components/business/ai-conversation-panel/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import AdvisorResult from './AdvisorResult.vue';

  import { getCustomerOptions, querySalesAdvisor, streamSalesAdvisor, uploadTempAttachment } from '@/api/modules';
  import useTypewriter from '@/hooks/useTypewriter';
  import useUserStore from '@/store/modules/user';

  const FEATURE_ADVISOR = 'ai_advisor';

  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  const message = ref('');
  const analyzing = ref(false);
  const streaming = ref(false);

  // 输入类型：分析对话（默认，保持原有能力） / 查客户数据（自然语言查本人及下级名下客户与跟进）
  type AdvisorMode = 'analyze' | 'query';
  const queryMode = ref<AdvisorMode>('analyze');
  const { text: streamingText, feed: feedStreaming, finish: finishStreaming, reset: resetStreaming } = useTypewriter();

  interface Pic {
    id: string;
    name: string;
  }
  const pics = ref<Pic[]>([]);

  // 联网查客户背景：开启后分析时按客户名/关键词先搜一轮公开信息并回传出处
  const enableWebSearch = ref(false);

  // 关联客户（可选）：分析结果会沉淀到该客户的 ai_analysis_result，供客户画像回读
  const customerId = ref<string | null>(null);
  const customerLoading = ref(false);
  const customerOptions = ref<{ label: string; value: string }[]>([]);

  async function loadCustomerOptions(keyword?: string) {
    customerLoading.value = true;
    try {
      const res = await getCustomerOptions({ current: 1, pageSize: 10, keyword });
      customerOptions.value = (res.list || []).map((item) => ({
        label: item.name,
        value: String(item.id),
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      customerLoading.value = false;
    }
  }

  function handleSearchCustomer(keyword: string) {
    loadCustomerOptions(keyword);
  }

  // 打开下拉时默认展示前 10 条，用户可输入关键字检索
  onMounted(() => {
    loadCustomerOptions();
  });

  interface ThreadItem {
    id: string;
    role: 'USER' | 'ASSISTANT';
    content: string;
    time?: number;
    userName?: string;
    payload?: SalesAdvisorAnalyzeResult | null;
    queryResult?: SalesAdvisorQueryResult | null;
    status?: string;
  }

  const thread = ref<ThreadItem[]>([]);
  const activeConversationId = ref('');
  const conversationPanelRef = ref<InstanceType<typeof AiConversationPanel>>();

  // 对话滚动区（DeepSeek 式：输入框固定底部，中间对话记录可滚动，新消息自动滚到底）
  const scrollContainerRef = ref<HTMLElement | null>(null);

  function isNearBottom(): boolean {
    const el = scrollContainerRef.value;
    if (!el) {
      return true;
    }
    return el.scrollHeight - el.scrollTop - el.clientHeight < 80;
  }

  async function scrollToBottom(force = false) {
    const el = scrollContainerRef.value;
    if (!el) {
      return;
    }
    if (!force && !isNearBottom()) {
      return;
    }
    await nextTick();
    el.scrollTop = el.scrollHeight;
  }

  // 新增消息（用户发送 / AI 回复）时强制滚到底；流式打字时仅在接近底部才跟随
  watch(
    () => thread.value.length,
    () => {
      scrollToBottom(true);
    }
  );
  watch(
    () => streamingText.value,
    () => {
      scrollToBottom(false);
    }
  );

  const canAnalyze = computed(() => {
    if (analyzing.value) {
      return false;
    }
    if (queryMode.value === 'query') {
      return message.value.trim() !== '';
    }
    return message.value.trim() !== '' || pics.value.length > 0;
  });
  const currentUserName = computed(() => userStore.userInfo.name || '');

  function formatTime(ts?: number): string {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '';
  }

  function parsePayload(payload?: string | null): SalesAdvisorAnalyzeResult | null {
    if (!payload) {
      return null;
    }
    try {
      return JSON.parse(payload) as SalesAdvisorAnalyzeResult;
    } catch {
      return null;
    }
  }

  // 查数消息的 payload 是 { columns, rows }；据此与结构化分析结果区分开
  function parseQueryPayload(payload?: string | null): SalesAdvisorQueryResult | null {
    if (!payload) {
      return null;
    }
    try {
      const parsed = JSON.parse(payload) as { columns?: string[]; rows?: unknown[][] };
      if (Array.isArray(parsed.columns)) {
        return {
          answer: '',
          queried: true,
          columns: parsed.columns,
          rows: (parsed.rows || []) as (string | number | boolean | null)[][],
        };
      }
      return null;
    } catch {
      return null;
    }
  }

  function toThreadItems(messages: AgentConversationMessage[]): ThreadItem[] {
    return (messages || []).map((msg) => {
      const item: ThreadItem = {
        id: msg.id,
        role: msg.role,
        content: msg.content || '',
        time: msg.createTime,
        userName: msg.role === 'USER' ? currentUserName.value : undefined,
        payload: null,
        queryResult: null,
        status: msg.status,
      };
      if (msg.role === 'ASSISTANT') {
        item.queryResult = parseQueryPayload(msg.payload);
        if (!item.queryResult) {
          item.payload = parsePayload(msg.payload);
        }
      }
      return item;
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

  async function customUpload({ file, onFinish, onError }: UploadCustomRequestOptions) {
    try {
      const res = await uploadTempAttachment(file.file as File);
      const id = res.data?.[0];
      if (!id) {
        throw new Error('upload empty');
      }
      pics.value.push({ id, name: file.name });
      onFinish();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      Message.error(t('common.operationFailed'));
      onError();
    }
  }

  function removePic(index: number) {
    pics.value.splice(index, 1);
  }

  async function handleAnalyzeStream() {
    analyzing.value = true;
    streaming.value = true;
    resetStreaming();

    const text = message.value.trim();
    const picIds = pics.value.map((pic) => pic.id);
    const userText = text || (picIds.length ? `【聊天记录截图】×${picIds.length}` : '');
    thread.value.push({
      id: `u_${Date.now()}`,
      role: 'USER',
      content: userText,
      time: Date.now(),
      userName: currentUserName.value,
    });
    // 发送后立即清空输入框与已上传截图，避免残留被下次分析重复提交
    message.value = '';
    pics.value = [];

    let assistantPushed = false;
    try {
      const stream = streamSalesAdvisor({
        message: text || undefined,
        picIds,
        conversationId: activeConversationId.value || undefined,
        customerId: customerId.value || undefined,
        enableWebSearch: enableWebSearch.value || undefined,
      });

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
          const payload = (event.data?.payload as SalesAdvisorAnalyzeResult) || null;
          thread.value.push({
            id: `a_${Date.now()}`,
            role: 'ASSISTANT',
            content: streamingText.value,
            time: Date.now(),
            payload,
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
      streaming.value = false;
      resetStreaming();
      analyzing.value = false;
    }
  }

  async function handleQuery() {
    const question = message.value.trim();
    if (!question) {
      return;
    }
    analyzing.value = true;
    streaming.value = true;
    resetStreaming();

    thread.value.push({
      id: `u_${Date.now()}`,
      role: 'USER',
      content: question,
      time: Date.now(),
      userName: currentUserName.value,
    });

    try {
      const result = await querySalesAdvisor({
        question,
        conversationId: activeConversationId.value || undefined,
      });
      activeConversationId.value = result.conversationId || activeConversationId.value;

      // 查数回答也走打字机逐字输出，避免一次性刷出一大段
      feedStreaming(result.answer || '');
      await finishStreaming();

      thread.value.push({
        id: `a_${Date.now()}`,
        role: 'ASSISTANT',
        content: streamingText.value,
        time: Date.now(),
        queryResult: result,
        status: 'done',
      });
      message.value = '';
      // 刷新左侧历史，让本次查数会话实时出现在列表里
      conversationPanelRef.value?.reload();
    } catch (error) {
      thread.value.push({
        id: `a_${Date.now()}`,
        role: 'ASSISTANT',
        content: (error as Error)?.message || t('common.operationFailed'),
        time: Date.now(),
        status: 'error',
      });
    } finally {
      streaming.value = false;
      resetStreaming();
      analyzing.value = false;
    }
  }

  async function handleAnalyze() {
    if (!canAnalyze.value) {
      return;
    }
    if (queryMode.value === 'query') {
      await handleQuery();
      return;
    }
    await handleAnalyzeStream();
  }

  const followDrawerVisible = ref(false);
  const followInitialValues = ref<Record<string, any>>({});
  const followFormKey = ref<FormDesignKeyEnum>(FormDesignKeyEnum.FOLLOW_RECORD);
  const followSourceId = ref<string | undefined>(undefined);
  const followOtherSaveParams = ref<Record<string, any>>({});
  const followInitialSourceName = ref('');
  const followLinkFormKey = computed(() =>
    followFormKey.value === FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER ? FormDesignKeyEnum.CUSTOMER : undefined
  );
  const followLinkScenario = computed(() =>
    followFormKey.value === FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER
      ? FormLinkScenarioEnum.CUSTOMER_TO_RECORD
      : undefined
  );

  function buildFollowContent(result: SalesAdvisorAnalyzeResult): string {
    if (!result) {
      return '';
    }
    const lines: string[] = [];
    if (result.intentScore) {
      lines.push(`意向评分：${result.intentScore}`);
    }
    if (result.signals?.length) {
      lines.push(`成交信号：${result.signals.join('；')}`);
    }
    if (result.objections?.length) {
      lines.push(`异议点：${result.objections.join('；')}`);
    }
    if (result.emotion) {
      lines.push(`情绪满意度：${result.emotion}`);
    }
    if (result.competitorMentions?.length) {
      lines.push(`竞品提及：${result.competitorMentions.join('；')}`);
    }
    if (result.churnRisk) {
      lines.push(`流失风险：${result.churnRisk}`);
    }
    if (result.suggestedScripts?.length) {
      lines.push(`候选话术：\n${result.suggestedScripts.map((script) => `- ${script}`).join('\n')}`);
    }
    return lines.join('\n');
  }

  function handleToFollow(result: SalesAdvisorAnalyzeResult) {
    if (!result) {
      return;
    }
    // 分析时已关联客户，则跟进记录直接落到该客户名下，无需再次选择客户
    if (customerId.value) {
      followFormKey.value = FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER;
      followSourceId.value = customerId.value;
      followOtherSaveParams.value = { customerId: customerId.value };
      followInitialSourceName.value = customerOptions.value.find((o) => o.value === customerId.value)?.label || '';
    } else {
      followFormKey.value = FormDesignKeyEnum.FOLLOW_RECORD;
      followSourceId.value = undefined;
      followOtherSaveParams.value = {};
      followInitialSourceName.value = '';
    }
    followInitialValues.value = { content: buildFollowContent(result) };
    followDrawerVisible.value = true;
  }

  function handleFollowSaved() {
    Message.success(t('common.operationSuccess'));
    followDrawerVisible.value = false;
  }

  function toQueryColumns(cols: string[] = []) {
    return cols.map((col) => ({ title: col, key: col, ellipsis: { tooltip: true } }));
  }

  function toQueryRows(result: SalesAdvisorQueryResult) {
    const cols = result.columns || [];
    return (result.rows || []).map((row) => {
      const obj: Record<string, unknown> = {};
      cols.forEach((col, i) => {
        obj[col] = row[i];
      });
      return obj;
    });
  }
</script>

<style scoped lang="scss">
  .advisor-composer {
    transition: border-color 0.2s ease;
    &:focus-within {
      border-color: var(--primary-8);
    }
  }
  .advisor-tool-button {
    padding: 2px 4px;
    height: 26px !important;
    border-radius: 4px;
    &:hover {
      background: var(--text-n9);
    }
  }
  .advisor-cursor {
    display: inline-block;
    margin-left: 2px;
    width: 8px;
    height: 14px;
    background: var(--primary-8);
    vertical-align: -2px;
    animation: advisor-blink 1s step-end infinite;
  }
  @keyframes advisor-blink {
    50% {
      opacity: 0;
    }
  }
</style>
