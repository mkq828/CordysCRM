<template>
  <div class="content-page flex h-full flex-col overflow-hidden p-[16px]">
    <CrmCard hide-footer class="content-card min-h-0 flex-1 overflow-y-auto">
      <template #title>
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
          <span class="text-[14px] font-semibold">{{ t('menu.aiContent') }}</span>
        </div>
      </template>

      <div class="flex flex-col gap-[16px] px-[24px] pb-[24px]">
        <div class="text-[13px] text-orange-500">{{ t('content.desc') }}</div>

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
          <span class="mt-[8px] w-[72px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('content.product') }}</span>
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

        <template v-if="contents.length || rawContent">
          <div class="flex items-center justify-between">
            <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('content.result') }}</span>
            <div v-if="contents.length" class="flex items-center gap-[8px]">
              <n-button size="tiny" quaternary @click="copyAll">{{ t('content.copyAll') }}</n-button>
              <n-button size="tiny" quaternary @click="exportText">{{ t('content.export') }}</n-button>
            </div>
          </div>

          <div v-if="rawContent && !contents.length" class="content-raw">
            <span class="content-raw-label">{{ t('content.rawFallback') }}</span>
            <span class="whitespace-pre-wrap">{{ rawContent }}</span>
          </div>

          <div v-if="contents.length" class="flex flex-col gap-[12px]">
            <div v-for="(item, index) in contents" :key="index" class="content-card-item">
              <div class="flex items-start gap-[10px]">
                <span
                  class="mt-[2px] flex h-[20px] w-[20px] shrink-0 items-center justify-center rounded-[6px] text-[12px] font-bold text-white"
                  style="background: linear-gradient(135deg, #f97316, #fb923c)"
                >
                  {{ index + 1 }}
                </span>
                <div class="flex min-w-0 flex-1 flex-col gap-[10px]">
                  <div v-if="item.topic" class="flex items-center gap-[8px]">
                    <span class="content-field-label">{{ t('content.topic') }}</span>
                    <span class="text-[14px] font-semibold text-[var(--text-n1)]">{{ item.topic }}</span>
                  </div>
                  <div v-if="item.copy" class="content-field">
                    <div class="flex items-center justify-between">
                      <span class="content-field-label">{{ t('content.copy') }}</span>
                      <n-button size="tiny" quaternary @click="copyItem(item.copy)">{{
                        t('content.copyItem')
                      }}</n-button>
                    </div>
                    <div class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
                      {{ item.copy }}
                    </div>
                  </div>
                  <div v-if="item.imageCopy" class="content-field">
                    <div class="flex items-center justify-between">
                      <span class="content-field-label">{{ t('content.imageCopy') }}</span>
                      <n-button size="tiny" quaternary @click="copyItem(item.imageCopy)">
                        {{ t('content.copyItem') }}
                      </n-button>
                    </div>
                    <div class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
                      {{ item.imageCopy }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <n-empty v-else-if="hasGenerated && !generating" :description="t('content.empty')" />
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NEmpty, NInput, NInputNumber, NSelect, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiContentItem } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { generateAiContent } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const industry = ref('');
  const product = ref('');
  const platform = ref<'douyin' | 'xiaohongshu' | 'moments'>('douyin');
  const topicCount = ref(5);
  const generating = ref(false);
  const hasGenerated = ref(false);
  const contents = ref<AiContentItem[]>([]);
  const rawContent = ref('');

  const platformOptions = computed(() => [
    { label: t('content.platformDouyin'), value: 'douyin' },
    { label: t('content.platformXiaohongshu'), value: 'xiaohongshu' },
    { label: t('content.platformMoments'), value: 'moments' },
  ]);

  const canGenerate = computed(() => !generating.value && industry.value.trim() !== '' && product.value.trim() !== '');

  async function handleGenerate() {
    if (!canGenerate.value) {
      return;
    }
    generating.value = true;
    hasGenerated.value = true;
    try {
      const res = await generateAiContent({
        industry: industry.value.trim(),
        product: product.value.trim(),
        platform: platform.value,
        topicCount: topicCount.value,
      });
      contents.value = res?.contents || [];
      rawContent.value = res?.rawContent || '';
    } catch (error) {
      contents.value = [];
      rawContent.value = '';
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      generating.value = false;
    }
  }

  function buildAllText(): string {
    return contents.value
      .map((item, index) =>
        [
          `【${t('content.topic')} ${index + 1}】${item.topic || ''}`,
          `${t('content.copy')}：${item.copy || ''}`,
          `${t('content.imageCopy')}：${item.imageCopy || ''}`,
        ].join('\n')
      )
      .join('\n\n');
  }

  async function writeClipboard(text?: string) {
    if (!text) {
      return;
    }
    try {
      await navigator.clipboard.writeText(text);
      Message.success(t('content.copied'));
    } catch {
      Message.error(t('common.operationFailed'));
    }
  }

  async function copyItem(text?: string) {
    await writeClipboard(text);
  }

  async function copyAll() {
    await writeClipboard(buildAllText());
  }

  function exportText() {
    const blob = new Blob([buildAllText()], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${t('menu.aiContent')}.txt`;
    link.click();
    URL.revokeObjectURL(url);
  }
</script>

<style lang="less" scoped>
  .content-page {
    background: #f5f7fa;
  }
  .content-card-item {
    padding: 14px 16px;
    border: 1px solid rgb(249 115 22 / 16%);
    border-radius: 8px;
    background: rgb(249 115 22 / 5%);
  }
  .content-field {
    padding: 8px 10px;
    border-radius: 6px;
    background: rgb(148 163 184 / 8%);
  }
  .content-field-label {
    flex-shrink: 0;
    font-size: 12px;
    font-weight: 500;
    color: var(--text-n3);
  }
  .content-raw {
    display: flex;
    padding: 8px 10px;
    font-size: 12px;
    border-radius: 6px;
    color: var(--text-n3);
    background: rgb(148 163 184 / 8%);
    gap: 8px;
    line-height: 1.5;
    .content-raw-label {
      flex-shrink: 0;
      font-weight: 500;
    }
  }
</style>
