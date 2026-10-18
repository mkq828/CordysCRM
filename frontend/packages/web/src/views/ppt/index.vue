<template>
  <div class="ppt-page flex h-full flex-col overflow-hidden p-[16px]">
    <CrmCard hide-footer no-content-padding content-height="100%" class="ppt-card min-h-0 flex-1 overflow-hidden">
      <template #title>
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
          <span class="text-[14px] font-semibold">{{ t('menu.aiPpt') }}</span>
        </div>
      </template>

      <n-empty v-if="available === false" class="ppt-locked" :description="t('ppt.locked')" />

      <CrmSplitPanel v-else class="h-full" :max="0.5" :min="0.2" :default-size="0.24">
        <template #1>
          <AiConversationPanel
            ref="conversationPanelRef"
            :feature-code="FEATURE_PPT"
            :active-id="activeConversationId"
            :generating-id="generating !== 'none' ? activeConversationId : ''"
            @select="handleSelectHistory"
            @new="handleNewConversation"
          />
        </template>

        <template #2>
          <div class="flex h-full flex-col overflow-y-auto px-[24px] pb-[24px]">
            <div class="text-[13px] text-orange-500">{{ t('ppt.desc') }}</div>

            <div class="mt-[16px] flex flex-col gap-[16px]">
              <div class="grid grid-cols-1 gap-[16px] md:grid-cols-2">
                <div class="flex items-center gap-[12px]">
                  <span class="w-[88px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('ppt.customerName') }}</span>
                  <n-input v-model:value="customerName" :placeholder="t('ppt.customerNamePlaceholder')" />
                </div>
                <div class="flex items-center gap-[12px]">
                  <span class="w-[88px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('ppt.industry') }}</span>
                  <n-input v-model:value="industry" :placeholder="t('ppt.industryPlaceholder')" />
                </div>
                <div class="flex items-center gap-[12px]">
                  <span class="w-[88px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('ppt.budget') }}</span>
                  <n-input v-model:value="budget" :placeholder="t('ppt.budgetPlaceholder')" />
                </div>
                <div class="flex items-center gap-[12px]">
                  <span class="w-[88px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('ppt.competitor') }}</span>
                  <n-input v-model:value="competitor" :placeholder="t('ppt.competitorPlaceholder')" />
                </div>
              </div>

              <div class="flex items-start gap-[12px]">
                <span class="mt-[8px] w-[88px] shrink-0 text-[13px] text-[var(--text-n2)]">
                  {{ t('ppt.product') }}
                </span>
                <n-input
                  v-model:value="product"
                  type="textarea"
                  :placeholder="t('ppt.productPlaceholder')"
                  :autosize="{ minRows: 2, maxRows: 5 }"
                />
              </div>

              <div class="flex items-start gap-[12px]">
                <span class="mt-[8px] w-[88px] shrink-0 text-[13px] text-[var(--text-n2)]">
                  {{ t('ppt.background') }}
                </span>
                <n-input
                  v-model:value="background"
                  type="textarea"
                  :placeholder="t('ppt.backgroundPlaceholder')"
                  :autosize="{ minRows: 4, maxRows: 8 }"
                />
              </div>

              <div class="flex items-start gap-[12px]">
                <span class="mt-[8px] w-[88px] shrink-0 text-[13px] text-[var(--text-n2)]">
                  {{ t('ppt.focus') }}
                </span>
                <n-input
                  v-model:value="focus"
                  type="textarea"
                  :placeholder="t('ppt.focusPlaceholder')"
                  :autosize="{ minRows: 2, maxRows: 5 }"
                />
              </div>

              <div class="flex items-center gap-[12px]">
                <span class="w-[88px] shrink-0" />
                <AiActionButton
                  :loading="generating === 'outline'"
                  :disabled="!canGenerate"
                  @click="handleGenerateOutline"
                >
                  {{ generating === 'outline' ? t('ppt.generatingOutline') : t('ppt.generateOutline') }}
                </AiActionButton>
              </div>
            </div>

            <div v-if="generating === 'outline' || outline" class="mt-[20px]">
              <div v-if="generating === 'outline'" class="ppt-blank">
                <div class="flex items-center gap-[8px]">
                  <CrmIcon
                    type="iconicon_loading"
                    :size="16"
                    color="linear-gradient(90deg, #f97316, #ec4899, #8b5cf6, #22d3ee)"
                    class="ai-loading-spin"
                  />
                  <ShimmerText size="16px">{{ t('ppt.generatingOutline') }}</ShimmerText>
                </div>
                <div class="text-[12px] text-orange-500">{{ t('ppt.generatingOutlineTip') }}</div>
                <div v-if="streamingText" class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
                  {{ streamingText }}
                </div>
              </div>
              <OutlineEditor
                v-else
                :title="outline?.title"
                :sections="outline?.sections || []"
                :raw-content="outline?.rawContent"
                :generating="generating === 'slides'"
                @generatePpt="handleGeneratePpt"
              />
            </div>

            <div v-if="generating === 'slides' || slides" class="mt-[20px]">
              <div v-if="generating === 'slides'" class="ppt-blank">
                <div class="flex items-center gap-[8px]">
                  <CrmIcon
                    type="iconicon_loading"
                    :size="16"
                    color="linear-gradient(90deg, #f97316, #ec4899, #8b5cf6, #22d3ee)"
                    class="ai-loading-spin"
                  />
                  <ShimmerText size="16px">{{ t('ppt.generatingPpt') }}</ShimmerText>
                </div>
                <div class="text-[12px] text-orange-500">{{ t('ppt.generatingPptTip') }}</div>
                <div v-if="streamingText" class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
                  {{ streamingText }}
                </div>
              </div>
              <SlidePreview
                v-else
                :title="slides?.title"
                :slides="slides?.slides || []"
                :raw-content="slides?.rawContent"
              />
            </div>
          </div>
        </template>
      </CrmSplitPanel>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  import { NEmpty, NInput, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AgentConversationDetail,
    AgentConversationMessage,
    AiPptOutlineResult,
    AiPptSection,
    AiPptSlidesResult,
  } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';
  import AiConversationPanel from '@/components/business/ai-conversation-panel/index.vue';
  import OutlineEditor from './components/OutlineEditor.vue';
  import SlidePreview from './components/SlidePreview.vue';

  import { getAiPptAvailable, streamGenerateAiPptOutline, streamGenerateAiPptSlides } from '@/api/modules';
  import useTypewriter from '@/hooks/useTypewriter';

  const FEATURE_PPT = 'ai_ppt';

  const { t } = useI18n();
  const Message = useMessage();

  const customerName = ref('');
  const industry = ref('');
  const product = ref('');
  const background = ref('');
  const budget = ref('');
  const competitor = ref('');
  const focus = ref('');

  const generating = ref<'none' | 'outline' | 'slides'>('none');
  const available = ref<boolean | null>(null);
  const { text: streamingText, feed: feedStreaming, reset: resetStreaming } = useTypewriter();
  const outline = ref<AiPptOutlineResult | null>(null);
  const slides = ref<AiPptSlidesResult | null>(null);
  const activeConversationId = ref('');
  const conversationPanelRef = ref<InstanceType<typeof AiConversationPanel>>();

  const canGenerate = computed(
    () =>
      generating.value === 'none' &&
      industry.value.trim() !== '' &&
      product.value.trim() !== '' &&
      background.value.trim() !== ''
  );

  function parsePayload(payload?: string | null): Record<string, unknown> | null {
    if (!payload) {
      return null;
    }
    try {
      return JSON.parse(payload) as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  function restoreFromMessages(messages: AgentConversationMessage[]): void {
    outline.value = null;
    slides.value = null;
    (messages || []).forEach((msg) => {
      if (msg.role !== 'ASSISTANT') {
        return;
      }
      const payload = parsePayload(msg.payload);
      if (!payload) {
        return;
      }
      if (Array.isArray(payload.sections)) {
        outline.value = payload as unknown as AiPptOutlineResult;
      }
      if (Array.isArray(payload.slides)) {
        slides.value = payload as unknown as AiPptSlidesResult;
      }
    });
  }

  function handleSelectHistory(detail: AgentConversationDetail): void {
    activeConversationId.value = detail?.conversation?.id || '';
    restoreFromMessages(detail?.messages || []);
  }

  function handleNewConversation(): void {
    activeConversationId.value = '';
    outline.value = null;
    slides.value = null;
  }

  onMounted(async () => {
    try {
      available.value = await getAiPptAvailable();
    } catch {
      available.value = false; // 拉取失败交给后端门控，前端按未开通展示升级提示
    }
  });

  async function handleGenerateOutline() {
    if (!canGenerate.value) {
      return;
    }
    generating.value = 'outline';
    resetStreaming();
    slides.value = null;

    try {
      const stream = streamGenerateAiPptOutline({
        customerName: customerName.value.trim(),
        industry: industry.value.trim(),
        product: product.value.trim(),
        background: background.value.trim(),
        budget: budget.value.trim(),
        competitor: competitor.value.trim(),
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
        } else if (event.type === 'done') {
          const result = event.data?.payload as AiPptOutlineResult | undefined;
          if (result) {
            outline.value = result;
          }
        }
      }
    } catch (error) {
      outline.value = null;
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      generating.value = 'none';
      resetStreaming();
    }
  }

  async function handleGeneratePpt(payload: { title?: string; sections: AiPptSection[] }) {
    generating.value = 'slides';
    resetStreaming();
    slides.value = null;

    try {
      const stream = streamGenerateAiPptSlides({
        title: payload.title,
        sections: payload.sections,
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
        } else if (event.type === 'done') {
          const result = event.data?.payload as AiPptSlidesResult | undefined;
          if (result) {
            slides.value = result;
          }
        }
      }
    } catch (error) {
      slides.value = null;
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      generating.value = 'none';
      resetStreaming();
    }
  }
</script>

<style lang="less" scoped>
  .ppt-page {
    background: #f5f7fa;
  }
  .ppt-locked {
    padding-top: 60px;
  }
  .ppt-blank {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 24px;
    min-height: 200px;
    border: 1px dashed rgb(249 115 22 / 30%);
    border-radius: 8px;
    background: rgb(249 115 22 / 4%);
    flex-direction: column;
    gap: 8px;
  }
</style>
