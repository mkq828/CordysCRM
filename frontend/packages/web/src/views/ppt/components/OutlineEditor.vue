<template>
  <div class="outline-editor flex flex-col gap-[12px]">
    <div class="flex items-center justify-between">
      <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('ppt.outlineTitle') }}</span>
      <div class="flex items-center gap-[8px]">
        <n-button size="tiny" quaternary @click="copyOutline">{{ t('ppt.copyOutline') }}</n-button>
        <n-button size="tiny" quaternary @click="downloadMd">{{ t('ppt.downloadMd') }}</n-button>
      </div>
    </div>

    <div class="text-[12px] text-orange-500">{{ t('ppt.editHint') }}</div>

    <div
      v-if="!sections.length && rawContent"
      class="outline-raw whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]"
    >
      {{ rawContent }}
    </div>

    <div v-if="sections.length" class="flex flex-col gap-[12px]">
      <div v-for="(section, index) in localSections" :key="section.key || index" class="outline-section">
        <div class="flex items-center gap-[10px]">
          <span
            class="flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-[6px] text-[12px] font-bold text-white"
            style="background: linear-gradient(135deg, #f97316, #fb923c)"
          >
            {{ index + 1 }}
          </span>
          <n-input v-model:value="section.title" size="small" class="outline-section__title" placeholder="标题" />
        </div>

        <n-input
          v-model:value="section.content"
          type="textarea"
          :autosize="{ minRows: 4, maxRows: 14 }"
          placeholder="区块内容（支持 Markdown）"
        />

        <n-collapse :default-expanded-names="[]" class="outline-section__preview">
          <n-collapse-item :title="t('ppt.preview')" name="preview">
            <div class="outline-markdown" v-html="render(section.content || '')" />
          </n-collapse-item>
        </n-collapse>
      </div>
    </div>

    <div class="flex justify-end">
      <AiActionButton :loading="generating" :disabled="!sections.length && !rawContent" @click="emitGenerate">
        {{ generating ? t('ppt.generatingPpt') : t('ppt.generatePpt') }}
      </AiActionButton>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { NButton, NCollapse, NCollapseItem, NInput, useMessage } from 'naive-ui';

  import { renderMarkdown } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { downloadByteFile } from '@lib/shared/method';
  import type { AiPptSection } from '@lib/shared/models/ai';

  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  const props = defineProps<{
    title?: string;
    sections: AiPptSection[];
    rawContent?: string;
    generating?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'generatePpt', payload: { title?: string; sections: AiPptSection[] }): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const localSections = ref<AiPptSection[]>([]);

  watch(
    () => props.sections,
    (sections) => {
      localSections.value = (sections || []).map((section) => ({ ...section }));
    },
    { immediate: true, deep: true }
  );

  function render(content: string): string {
    return renderMarkdown(content, { copyText: t('common.copy') });
  }

  function buildMarkdown(): string {
    const body = localSections.value.flatMap((section) => ['', `## ${section.title || ''}`, '', section.content || '']);
    return [`# ${props.title || t('ppt.outlineTitle')}`, ...body].join('\n');
  }

  async function writeClipboard(text?: string) {
    if (!text) {
      return;
    }
    try {
      await navigator.clipboard.writeText(text);
      Message.success(t('ppt.copied'));
    } catch {
      Message.error(t('common.operationFailed'));
    }
  }

  async function copyOutline() {
    await writeClipboard(buildMarkdown());
  }

  function downloadMd() {
    downloadByteFile(buildMarkdown(), `${props.title || t('ppt.outlineTitle')}.md`);
  }

  function emitGenerate() {
    emit('generatePpt', {
      title: props.title,
      sections: localSections.value.map((section) => ({ ...section })),
    });
  }
</script>

<style lang="less" scoped>
  .outline-section {
    display: flex;
    padding: 14px 16px;
    border: 1px solid rgb(249 115 22 / 16%);
    border-radius: 8px;
    background: rgb(249 115 22 / 5%);
    flex-direction: column;
    gap: 10px;
  }
  .outline-section__title {
    :deep(.n-input__input-el) {
      font-weight: 600;
    }
  }
  .outline-section__preview {
    :deep(.n-collapse-item__header-main) {
      font-size: 12px;
      color: var(--text-n3);
    }
  }
  .outline-markdown {
    :deep(h1),
    :deep(h2),
    :deep(h3) {
      margin: 12px 0 8px;
      font-weight: 600;
      color: var(--text-n1);
    }
    :deep(p) {
      margin: 0 0 8px;
    }
    :deep(ul) {
      padding-left: 20px;
      list-style: disc;
    }
    :deep(code) {
      padding: 2px 4px;
      font-size: 12px;
      border-radius: 4px;
      background: var(--text-n8);
    }
    :deep(pre) {
      overflow: auto;
      padding: 8px 10px;
      border-radius: 6px;
      background: var(--text-n9);
    }
  }
  .outline-raw {
    padding: 10px 12px;
    border-radius: 6px;
    background: rgb(148 163 184 / 8%);
  }
</style>
