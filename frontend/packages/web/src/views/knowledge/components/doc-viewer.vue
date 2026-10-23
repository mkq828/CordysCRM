<template>
  <n-modal
    v-model:show="show"
    preset="card"
    :title="doc?.name"
    class="doc-viewer-modal"
    :bordered="false"
    :style="{ width: 'min(900px, 90vw)' }"
  >
    <div class="doc-viewer-body h-[70vh] overflow-auto">
      <div v-if="loading" class="flex h-full items-center justify-center text-[var(--text-n4)]">
        {{ t('knowledge.viewLoading') }}
      </div>
      <div v-else-if="error" class="flex h-full items-center justify-center text-[var(--text-n4)]">{{ error }}</div>
      <template v-else>
        <iframe v-if="type === 'pdf'" :src="pdfUrl" class="h-full w-full border-0" />
        <div v-else-if="type === 'docx'" class="doc-html px-[16px] py-[8px]" v-html="htmlContent" />
        <pre v-else class="whitespace-pre-wrap px-[16px] py-[8px] text-[var(--text-n2)]">{{ textContent }}</pre>
      </template>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
  import { NModal } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiKnowledgeDoc } from '@lib/shared/models/ai';

  import { downloadAttachment } from '@/api/modules';

  import DOMPurify from 'dompurify';
  import mammoth from 'mammoth';

  const props = defineProps<{ show: boolean; doc: AiKnowledgeDoc | null }>();
  const emit = defineEmits<{ (e: 'update:show', v: boolean): void }>();

  const { t } = useI18n();

  const show = computed({
    get: () => props.show,
    set: (v: boolean) => emit('update:show', v),
  });

  const loading = ref(false);
  const error = ref('');
  const pdfUrl = ref('');
  const htmlContent = ref('');
  const textContent = ref('');
  const type = ref('');

  async function load(doc: AiKnowledgeDoc) {
    if (!doc.attachmentId) {
      type.value = doc.fileType?.toLowerCase() ?? '';
      error.value = t('knowledge.viewNoFile');
      return;
    }
    loading.value = true;
    error.value = '';
    if (pdfUrl.value) URL.revokeObjectURL(pdfUrl.value);
    pdfUrl.value = '';
    htmlContent.value = '';
    textContent.value = '';
    type.value = doc.fileType?.toLowerCase() ?? '';
    try {
      const res = await downloadAttachment(doc.attachmentId);
      const blob = new Blob([res]);
      if (type.value === 'pdf') {
        pdfUrl.value = URL.createObjectURL(blob);
      } else if (type.value === 'docx') {
        const arrayBuffer = await blob.arrayBuffer();
        const result = await mammoth.convertToHtml({ arrayBuffer });
        htmlContent.value = DOMPurify.sanitize(result.value);
      } else {
        textContent.value = await blob.text();
      }
    } catch (e) {
      error.value = t('knowledge.viewFailed');
    } finally {
      loading.value = false;
    }
  }

  watch(
    () => [props.show, props.doc] as const,
    ([visible, doc]) => {
      if (visible && doc) load(doc);
    }
  );
</script>

<style lang="less" scoped>
  .doc-html {
    :deep(img) {
      max-width: 100%;
    }
    :deep(table) {
      border-collapse: collapse;
    }
    :deep(td),
    :deep(th) {
      padding: 4px 8px;
      border: 1px solid var(--text-n8);
    }
  }
</style>
