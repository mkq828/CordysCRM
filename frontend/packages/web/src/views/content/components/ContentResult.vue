<template>
  <div v-if="contents.length || rawContent" class="flex flex-col gap-[12px]">
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
            <div v-for="field in itemTextFields(item)" :key="field.label" class="content-field">
              <div class="flex items-center justify-between">
                <span class="content-field-label">{{ field.label }}</span>
                <n-button size="tiny" quaternary @click="copyItem(field.value)">{{ t('content.copyItem') }}</n-button>
              </div>
              <div class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
                {{ field.value }}
              </div>
            </div>
            <div v-if="item.hashtags?.length" class="content-field flex flex-wrap items-center gap-[6px]">
              <span class="content-field-label">{{ t('content.hashtags') }}</span>
              <n-tag v-for="tag in item.hashtags" :key="tag" size="small" type="warning" :bordered="false">
                {{ tag }}
              </n-tag>
            </div>
            <div v-if="item.bestTime" class="content-field flex items-center gap-[8px]">
              <span class="content-field-label">{{ t('content.bestTime') }}</span>
              <span class="text-[13px] text-[var(--text-n2)]">{{ item.bestTime }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NTag, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiContentItem } from '@lib/shared/models/ai';

  const props = defineProps<{
    contents: AiContentItem[];
    rawContent?: string;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  // 每条内容中需要展示 + 可复制的文本字段，按固定顺序渲染
  function itemTextFields(item: AiContentItem) {
    const fields: { label: string; value: string }[] = [];
    if (item.title) {
      fields.push({ label: t('content.title'), value: item.title });
    }
    if (item.copy) {
      fields.push({ label: t('content.copy'), value: item.copy });
    }
    if (item.imageCopy) {
      fields.push({ label: t('content.imageCopy'), value: item.imageCopy });
    }
    if (item.coverCopy) {
      fields.push({ label: t('content.coverCopy'), value: item.coverCopy });
    }
    if (item.script) {
      fields.push({ label: t('content.script'), value: item.script });
    }
    return fields;
  }

  function buildAllText(): string {
    return props.contents
      .map((item, index) => {
        const lines = [`【${t('content.topic')} ${index + 1}】${item.topic || ''}`];
        if (item.title) {
          lines.push(`${t('content.title')}：${item.title}`);
        }
        if (item.copy) {
          lines.push(`${t('content.copy')}：${item.copy}`);
        }
        if (item.imageCopy) {
          lines.push(`${t('content.imageCopy')}：${item.imageCopy}`);
        }
        if (item.coverCopy) {
          lines.push(`${t('content.coverCopy')}：${item.coverCopy}`);
        }
        if (item.hashtags?.length) {
          lines.push(`${t('content.hashtags')}：${item.hashtags.join(' ')}`);
        }
        if (item.bestTime) {
          lines.push(`${t('content.bestTime')}：${item.bestTime}`);
        }
        if (item.script) {
          lines.push(`${t('content.script')}：${item.script}`);
        }
        return lines.join('\n');
      })
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
