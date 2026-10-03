<template>
  <div class="h-[640px]">
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
            :feature-code="FEATURE_ADVISOR"
            :active-id="activeConversationId"
            @select="handleSelectHistory"
            @new="handleNewConversation"
          />
        </template>

        <template #2>
          <div class="flex h-full flex-col overflow-y-auto px-[24px] pb-[24px]">
            <div class="text-[13px] text-orange-500">{{ t('workbench.smart.advisorDesc') }}</div>

            <div class="mt-[16px] flex flex-col gap-[16px]">
              <n-input
                v-model:value="message"
                type="textarea"
                :placeholder="t('workbench.smart.advisorPlaceholder')"
                :autosize="{ minRows: 3, maxRows: 10 }"
              />

              <div class="flex flex-wrap items-center gap-[8px]">
                <n-upload :custom-request="customUpload" :show-file-list="false" accept="image/*" multiple>
                  <n-button size="small" :disabled="analyzing">
                    <template #icon>
                      <CrmIcon type="iconicon_link1" :size="16" />
                    </template>
                    {{ t('workbench.smart.advisorUploadTip') }}
                  </n-button>
                </n-upload>
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

              <div>
                <n-button type="primary" :loading="analyzing" :disabled="!canAnalyze" @click="handleAnalyze">
                  {{ analyzing ? t('workbench.smart.advisorAnalyzing') : t('workbench.smart.advisorAnalyze') }}
                </n-button>
              </div>
            </div>

            <div class="mt-[20px] flex flex-col gap-[16px]">
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
                    <AdvisorResult v-if="item.payload" :result="item.payload" />
                    <div v-if="item.payload" class="flex justify-end">
                      <n-button size="small" type="primary" @click="handleToFollow(item.payload)">
                        {{ t('workbench.smart.advisorToFollow') }}
                      </n-button>
                    </div>
                  </template>
                </div>
              </template>

              <div v-if="streaming" class="flex flex-col gap-[8px] rounded-[6px] bg-[var(--fill-2)] p-[12px]">
                <div class="flex items-center gap-[8px] text-[13px] text-[var(--text-n3)]">
                  <CrmIcon type="iconicon_loading" :size="16" class="animate-spin" />
                  {{ t('workbench.smart.advisorAnalyzing') }}
                </div>
                <div v-if="streamingText" class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
                  {{ streamingText }}
                </div>
              </div>
            </div>
          </div>
        </template>
      </CrmSplitPanel>

      <CrmFormCreateDrawer
        v-model:visible="followDrawerVisible"
        :form-key="FormDesignKeyEnum.FOLLOW_RECORD"
        :initial-values="followInitialValues"
        @saved="handleFollowSaved"
      />
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { NButton, NEmpty, NInput, NTag, NUpload, type UploadCustomRequestOptions, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AgentConversationDetail,
    AgentConversationMessage,
    SalesAdvisorAnalyzeResult,
  } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import AiConversationPanel from '@/components/business/ai-conversation-panel/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import AdvisorResult from './AdvisorResult.vue';

  import { streamSalesAdvisor, uploadTempAttachment } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  const FEATURE_ADVISOR = 'ai_advisor';

  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  const message = ref('');
  const analyzing = ref(false);
  const streaming = ref(false);
  const streamingText = ref('');

  interface Pic {
    id: string;
    name: string;
  }
  const pics = ref<Pic[]>([]);

  interface ThreadItem {
    id: string;
    role: 'USER' | 'ASSISTANT';
    content: string;
    time?: number;
    userName?: string;
    payload?: SalesAdvisorAnalyzeResult | null;
    status?: string;
  }

  const thread = ref<ThreadItem[]>([]);
  const activeConversationId = ref('');

  const canAnalyze = computed(() => !analyzing.value && (message.value.trim() !== '' || pics.value.length > 0));
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

  function toThreadItems(messages: AgentConversationMessage[]): ThreadItem[] {
    return (messages || []).map((msg) => ({
      id: msg.id,
      role: msg.role,
      content: msg.content || '',
      time: msg.createTime,
      userName: msg.role === 'USER' ? currentUserName.value : undefined,
      payload: msg.role === 'ASSISTANT' ? parsePayload(msg.payload) : null,
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

  async function handleAnalyze() {
    if (!canAnalyze.value) {
      return;
    }
    analyzing.value = true;
    streaming.value = true;
    streamingText.value = '';

    const userText = message.value.trim() || (pics.value.length ? `【聊天记录截图】×${pics.value.length}` : '');
    thread.value.push({
      id: `u_${Date.now()}`,
      role: 'USER',
      content: userText,
      time: Date.now(),
      userName: currentUserName.value,
    });

    let assistantPushed = false;
    try {
      const stream = streamSalesAdvisor({
        message: message.value.trim() || undefined,
        picIds: pics.value.map((pic) => pic.id),
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
      streamingText.value = '';
      analyzing.value = false;
    }
  }

  const followDrawerVisible = ref(false);
  const followInitialValues = ref<Record<string, any>>({});

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
    followInitialValues.value = { content: buildFollowContent(result) };
    followDrawerVisible.value = true;
  }

  function handleFollowSaved() {
    Message.success(t('common.operationSuccess'));
    followDrawerVisible.value = false;
  }
</script>

<style scoped></style>
