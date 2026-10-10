<template>
  <div class="voiceclone-page">
    <!-- 页头 -->
    <div class="voiceclone-head">
      <h1 class="voiceclone-title">{{ t('voiceClone.title') }}</h1>
      <p class="voiceclone-desc">{{ t('voiceClone.desc') }}</p>
    </div>

    <!-- 未开通 -->
    <n-empty v-if="available === false" class="voiceclone-locked" :description="t('voiceClone.locked')" />

    <template v-else>
      <!-- 提交区 -->
      <div class="voiceclone-create">
        <div class="voiceclone-create-name">
          <label class="voiceclone-label">{{ t('voiceClone.nameLabel') }}</label>
          <n-input v-model:value="voiceName" :placeholder="t('voiceClone.namePlaceholder')" clearable />
        </div>
        <div class="voiceclone-create-sample">
          <label class="voiceclone-label">{{ t('voiceClone.sampleLabel') }}</label>
          <n-upload
            v-model:file-list="fileList"
            :max="1"
            accept=".wav,.mp3,.m4a"
            :custom-request="uploadSample"
            @remove="handleRemoveSample"
          >
            <n-button>{{ t('voiceClone.samplePlaceholder') }}</n-button>
          </n-upload>
          <p class="voiceclone-sample-tip">{{ t('voiceClone.sampleTip') }}</p>
        </div>
        <AiActionButton :loading="submitting" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? t('voiceClone.submitting') : t('voiceClone.submit') }}
        </AiActionButton>
      </div>

      <!-- 音色列表 -->
      <div class="voiceclone-list">
        <n-empty v-if="!voices.length" class="voiceclone-empty" :description="t('voiceClone.empty')" />
        <div v-for="voice in voices" :key="voice.id" class="voiceclone-card">
          <div class="voiceclone-card-main">
            <div class="voiceclone-card-head">
              <span class="voiceclone-card-name">{{ voice.name }}</span>
              <span v-if="voice.status === 'SUCCESS'" class="voiceclone-tag voiceclone-tag-success">
                {{ t('voiceClone.success') }}
              </span>
              <span v-else-if="voice.status === 'FAILED'" class="voiceclone-tag voiceclone-tag-failed">
                {{ t('voiceClone.failed') }}
              </span>
              <span v-else class="voiceclone-tag voiceclone-tag-training">
                <ShimmerText size="12px">{{ t('voiceClone.training') }}</ShimmerText>
              </span>
            </div>
            <p v-if="voice.errorMsg" class="voiceclone-card-error">{{ voice.errorMsg }}</p>
          </div>
          <div class="voiceclone-card-actions">
            <n-button
              v-if="voice.status === 'SUCCESS'"
              size="small"
              type="primary"
              ghost
              :loading="previewingId === voice.id"
              @click="handlePreview(voice)"
            >
              {{ previewingId === voice.id ? t('voiceClone.previewing') : t('voiceClone.preview') }}
            </n-button>
            <n-button size="small" quaternary type="error" @click="handleDelete(voice)">
              {{ t('voiceClone.delete') }}
            </n-button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref } from 'vue';
  import {
    NButton,
    NEmpty,
    NInput,
    NUpload,
    type UploadCustomRequestOptions,
    type UploadFileInfo,
    useDialog,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiCloneVoice } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  import {
    deleteAiVoiceCloneTask,
    getAiVoiceCloneAvailable,
    getAiVoiceCloneTasks,
    previewAiVoiceClone,
    submitAiVoiceClone,
    uploadTempAttachment,
  } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();
  const dialog = useDialog();

  const available = ref<boolean | null>(null);
  const voiceName = ref('');
  const fileList = ref<UploadFileInfo[]>([]);
  const sampleAttachmentId = ref('');
  const submitting = ref(false);
  const voices = ref<AiCloneVoice[]>([]);
  const previewingId = ref('');

  let pollTimer: number | undefined;

  async function uploadSample({ file, onFinish, onError }: UploadCustomRequestOptions) {
    try {
      const f = file as UploadFileInfo;
      const res = await uploadTempAttachment(f.file as File);
      const [attachmentId] = res.data;
      sampleAttachmentId.value = attachmentId;
      f.status = 'finished';
      onFinish();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
      onError();
    }
  }

  function handleRemoveSample() {
    sampleAttachmentId.value = '';
  }

  function hasTraining(): boolean {
    return voices.value.some((v) => v.status === 'TRAINING');
  }

  async function loadVoices() {
    try {
      voices.value = await getAiVoiceCloneTasks();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  function stopPolling() {
    if (pollTimer !== undefined) {
      window.clearInterval(pollTimer);
      pollTimer = undefined;
    }
  }

  function startPolling() {
    stopPolling();
    if (!hasTraining()) {
      return;
    }
    pollTimer = window.setInterval(async () => {
      if (!hasTraining()) {
        stopPolling();
        return;
      }
      await loadVoices();
    }, 4000);
  }

  function playBase64Wav(base64: string) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i += 1) {
      bytes[i] = binary.charCodeAt(i);
    }
    const blob = new Blob([bytes], { type: 'audio/wav' });
    const url = URL.createObjectURL(blob);
    const audio = new Audio(url);
    audio.onended = () => URL.revokeObjectURL(url);
    audio.play().catch(() => URL.revokeObjectURL(url));
  }

  async function handlePreview(voice: AiCloneVoice) {
    if (!voice.id || previewingId.value) {
      return;
    }
    previewingId.value = voice.id;
    try {
      const res = await previewAiVoiceClone(voice.id);
      if (res.audio) {
        playBase64Wav(res.audio);
      }
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      previewingId.value = '';
    }
  }

  async function handleSubmit() {
    const name = voiceName.value.trim();
    if (!name) {
      Message.warning(t('voiceClone.nameRequired'));
      return;
    }
    if (!sampleAttachmentId.value) {
      Message.warning(t('voiceClone.sampleRequired'));
      return;
    }
    if (submitting.value) {
      return;
    }
    submitting.value = true;
    try {
      await submitAiVoiceClone({ name, sampleAttachmentId: sampleAttachmentId.value });
      voiceName.value = '';
      sampleAttachmentId.value = '';
      fileList.value = [];
      Message.success(t('common.operationSuccess'));
      await loadVoices();
      startPolling();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      submitting.value = false;
    }
  }

  function handleDelete(voice: AiCloneVoice) {
    if (!voice.id) {
      return;
    }
    dialog.warning({
      title: t('common.confirmDelete'),
      content: t('voiceClone.deleteConfirm'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiVoiceCloneTask(voice.id);
          Message.success(t('common.operationSuccess'));
          await loadVoices();
        } catch (error) {
          Message.error((error as Error)?.message || t('common.operationFailed'));
        }
      },
    });
  }

  onMounted(async () => {
    try {
      available.value = await getAiVoiceCloneAvailable();
      if (available.value) {
        await loadVoices();
        startPolling();
      }
    } catch {
      available.value = false;
    }
  });

  onUnmounted(() => {
    stopPolling();
  });
</script>

<style scoped>
  .voiceclone-page {
    overflow: auto;
    padding: 24px;
    height: 100%;
  }
  .voiceclone-head {
    margin-bottom: 20px;
  }
  .voiceclone-title {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    color: var(--text-n1, #1f2937);
  }
  .voiceclone-desc {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--text-n2, #6b7280);
  }
  .voiceclone-locked {
    margin-top: 60px;
  }
  .voiceclone-create {
    display: flex;
    align-items: flex-end;
    gap: 20px;
    padding: 20px;
    border-radius: 12px;
    background: #ffffff;
    box-shadow: 0 1px 3px rgb(0 0 0 / 6%);
    flex-wrap: wrap;
  }
  .voiceclone-create-name {
    flex: 1;
    min-width: 220px;
  }
  .voiceclone-create-sample {
    flex: 1;
    min-width: 260px;
  }
  .voiceclone-label {
    display: block;
    margin-bottom: 8px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-n1, #1f2937);
  }
  .voiceclone-sample-tip {
    margin: 6px 0 0;
    font-size: 12px;
    line-height: 1.6;
    color: #f97316;
  }
  .voiceclone-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-top: 20px;
  }
  .voiceclone-empty {
    padding: 40px 0;
    border-radius: 12px;
    background: #ffffff;
  }
  .voiceclone-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    border-radius: 12px;
    background: #ffffff;
    box-shadow: 0 1px 3px rgb(0 0 0 / 6%);
    gap: 16px;
  }
  .voiceclone-card-main {
    min-width: 0;
    flex: 1;
  }
  .voiceclone-card-head {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .voiceclone-card-name {
    overflow: hidden;
    font-size: 15px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-n1, #1f2937);
  }
  .voiceclone-tag {
    padding: 2px 8px;
    font-size: 12px;
    line-height: 18px;
    border-radius: 4px;
    flex-shrink: 0;
  }
  .voiceclone-tag-success {
    color: #16a34a;
    background: #f0fdf4;
  }
  .voiceclone-tag-failed {
    color: #dc2626;
    background: #fef2f2;
  }
  .voiceclone-tag-training {
    color: #f97316;
    background: #fff7ed;
  }
  .voiceclone-card-error {
    margin: 6px 0 0;
    font-size: 12px;
    color: #dc2626;
  }
  .voiceclone-card-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }
</style>
