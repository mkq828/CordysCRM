<template>
  <div class="replica-page">
    <!-- 页头 -->
    <div class="replica-head">
      <h1 class="replica-title">{{ t('replica.title') }}</h1>
      <p class="replica-desc">{{ t('replica.desc') }}</p>
    </div>

    <!-- 未开通 -->
    <n-empty v-if="available === false" class="replica-locked" :description="t('replica.locked')" />

    <template v-else>
      <!-- 提交区 -->
      <div class="replica-create">
        <div class="replica-create-video">
          <label class="replica-label">{{ t('replica.videoLabel') }}</label>
          <n-upload
            v-model:file-list="fileList"
            :max="1"
            accept=".mp4,.mov,.m4v,.webm"
            :custom-request="uploadVideo"
            @remove="handleRemoveVideo"
          >
            <n-button>{{ t('replica.videoPlaceholder') }}</n-button>
          </n-upload>
          <p class="replica-tip">{{ t('replica.videoTip') }}</p>
        </div>
        <div class="replica-create-count">
          <label class="replica-label">{{ t('replica.countLabel') }}</label>
          <n-select v-model:value="variantCount" :options="countOptions" style="width: 120px" />
        </div>
        <AiActionButton :loading="submitting" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? t('replica.submitting') : t('replica.submit') }}
        </AiActionButton>
      </div>

      <!-- 任务列表 -->
      <div class="replica-list">
        <n-empty v-if="!tasks.length" class="replica-empty" :description="t('replica.empty')" />
        <div v-for="task in tasks" :key="task.id" class="replica-task">
          <div class="replica-task-head">
            <img
              v-if="task.coverAttachmentId"
              :src="previewUrl(task.coverAttachmentId)"
              class="replica-task-cover"
              alt=""
            />
            <div v-else class="replica-task-cover replica-task-cover-empty">
              <ShimmerText v-if="isTaskRunning(task)" size="12px">{{ t('replica.parsing') }}</ShimmerText>
              <span v-else>{{ t('replica.noCover') }}</span>
            </div>
            <div class="replica-task-meta">
              <span class="replica-task-name">{{ task.title || task.id }}</span>
              <span class="replica-task-sub"> {{ t('replica.duration') }}：{{ formatDuration(task.durationMs) }} </span>
            </div>
            <div class="replica-task-status">
              <span v-if="task.status === 'READY'" class="replica-tag replica-tag-success">{{
                t('replica.ready')
              }}</span>
              <span v-else-if="task.status === 'FAILED'" class="replica-tag replica-tag-failed">{{
                t('replica.failed')
              }}</span>
              <span v-else class="replica-tag replica-tag-running">
                <ShimmerText size="12px">{{ t('replica.running') }}</ShimmerText>
              </span>
            </div>
            <div class="replica-task-actions">
              <n-button
                v-if="task.status === 'READY'"
                size="small"
                type="primary"
                ghost
                :loading="regeneratingId === task.id"
                @click="handleRegenerate(task)"
              >
                {{ t('replica.regenerate') }}
              </n-button>
              <n-button size="small" quaternary type="error" @click="handleDeleteTask(task)">
                {{ t('replica.delete') }}
              </n-button>
            </div>
          </div>
          <p v-if="task.errorMsg" class="replica-task-error">{{ task.errorMsg }}</p>

          <!-- 变体卡片 -->
          <div v-if="task.variants?.length" class="replica-variants">
            <div v-for="variant in task.variants" :key="variant.id" class="replica-variant">
              <div class="replica-variant-main">
                <div class="replica-variant-head">
                  <span class="replica-variant-no">{{ t('replica.variantNo', { n: variant.variantNo ?? 0 }) }}</span>
                  <span v-if="variant.ratio" class="replica-variant-ratio">{{ variant.ratio }}</span>
                  <span v-if="variant.subtitleStyle" class="replica-variant-style">{{
                    styleLabel(variant.subtitleStyle)
                  }}</span>
                  <span v-if="variant.status === 'DONE'" class="replica-tag replica-tag-success">{{
                    t('replica.done')
                  }}</span>
                  <span v-else-if="variant.status === 'FAILED'" class="replica-tag replica-tag-failed">{{
                    t('replica.failed')
                  }}</span>
                  <span v-else class="replica-tag replica-tag-running">
                    <ShimmerText size="12px">{{ t('replica.composing') }}</ShimmerText>
                  </span>
                </div>
                <p v-if="variant.hookText" class="replica-variant-hook">{{ variant.hookText }}</p>
                <p v-if="variant.titleText" class="replica-variant-title">{{ variant.titleText }}</p>
                <p v-if="variant.errorMsg" class="replica-task-error">{{ variant.errorMsg }}</p>
              </div>
              <div class="replica-variant-actions">
                <n-button v-if="variant.status === 'DONE'" size="small" @click="openPreview(variant)">
                  {{ t('replica.preview') }}
                </n-button>
                <n-button
                  v-if="variant.status === 'DONE'"
                  size="small"
                  type="primary"
                  ghost
                  @click="handleDownload(variant)"
                >
                  {{ t('replica.download') }}
                </n-button>
                <n-button size="small" quaternary type="error" @click="handleDeleteVariant(variant)">
                  {{ t('replica.delete') }}
                </n-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 预览弹窗 -->
    <n-modal v-model:show="previewVisible" preset="card" :title="previewTitle" class="replica-preview-modal">
      <video v-if="previewUrlValue" :src="previewUrlValue" controls autoplay class="replica-preview-video" />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, onUnmounted, ref } from 'vue';
  import {
    NButton,
    NEmpty,
    NModal,
    NSelect,
    NUpload,
    type SelectOption,
    type UploadCustomRequestOptions,
    type UploadFileInfo,
    useDialog,
    useMessage,
  } from 'naive-ui';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiReplicaTask, AiReplicaVariant } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  import {
    createAiReplicaTask,
    deleteAiReplicaTask,
    deleteAiReplicaVariant,
    downloadAttachment,
    getAiReplicaAvailable,
    getAiReplicaTasks,
    regenerateAiReplicaTask,
    uploadTempAttachment,
  } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  const { t } = useI18n();
  const Message = useMessage();
  const dialog = useDialog();
  const userStore = useUserStore();

  const SUBTITLE_STYLES: Record<string, string> = {
    classic: '经典白',
    karaoke_yellow: '黄色卡拉OK',
    karaoke_green: '绿色卡拉OK',
    karaoke_pink: '粉色卡拉OK',
  };

  const available = ref<boolean | null>(null);
  const fileList = ref<UploadFileInfo[]>([]);
  const referenceAttachmentId = ref('');
  const variantCount = ref(3);
  const submitting = ref(false);
  const tasks = ref<AiReplicaTask[]>([]);
  const regeneratingId = ref('');
  const previewVisible = ref(false);
  const previewUrlValue = ref('');
  const previewTitle = ref('');

  let pollTimer: number | undefined;

  const countOptions = computed<SelectOption[]>(() => [1, 2, 3, 4, 5].map((n) => ({ label: `${n}`, value: n })));

  async function uploadVideo({ file, onFinish, onError }: UploadCustomRequestOptions) {
    try {
      const f = file as UploadFileInfo;
      const res = await uploadTempAttachment(f.file as File);
      const [attachmentId] = res.data;
      referenceAttachmentId.value = attachmentId;
      f.status = 'finished';
      onFinish();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
      onError();
    }
  }

  function handleRemoveVideo() {
    referenceAttachmentId.value = '';
  }

  async function loadTasks() {
    try {
      tasks.value = await getAiReplicaTasks();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  function hasRunning(): boolean {
    return tasks.value.some((task) => task.status !== 'READY' && task.status !== 'FAILED');
  }

  function stopPolling() {
    if (pollTimer !== undefined) {
      window.clearInterval(pollTimer);
      pollTimer = undefined;
    }
  }

  function startPolling() {
    stopPolling();
    pollTimer = window.setInterval(async () => {
      if (!hasRunning()) {
        stopPolling();
        return;
      }
      await loadTasks();
    }, 3000);
  }

  function isTaskRunning(task: AiReplicaTask): boolean {
    return task.status !== 'READY' && task.status !== 'FAILED';
  }

  function styleLabel(style?: string): string {
    if (!style) {
      return '';
    }
    return SUBTITLE_STYLES[style] ?? style;
  }

  function formatDuration(ms?: number): string {
    if (!ms) {
      return '--';
    }
    const seconds = Math.round(ms / 1000);
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return m > 0 ? `${m}${t('replica.min')}${s}${t('replica.sec')}` : `${s}${t('replica.sec')}`;
  }

  function previewUrl(id?: string): string {
    if (!id) {
      return '';
    }
    return `${PreviewAttachmentUrl}/${id}?userId=${userStore.userInfo.id}`;
  }

  async function handleSubmit() {
    if (!referenceAttachmentId.value) {
      Message.warning(t('replica.videoRequired'));
      return;
    }
    if (submitting.value) {
      return;
    }
    submitting.value = true;
    try {
      await createAiReplicaTask({
        referenceAttachmentId: referenceAttachmentId.value,
        variantCount: variantCount.value,
      });
      referenceAttachmentId.value = '';
      fileList.value = [];
      Message.success(t('common.operationSuccess'));
      await loadTasks();
      startPolling();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      submitting.value = false;
    }
  }

  async function handleRegenerate(task: AiReplicaTask) {
    if (!task.id || regeneratingId.value) {
      return;
    }
    regeneratingId.value = task.id;
    try {
      await regenerateAiReplicaTask(task.id);
      Message.success(t('common.operationSuccess'));
      await loadTasks();
      startPolling();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      regeneratingId.value = '';
    }
  }

  function openPreview(variant: AiReplicaVariant) {
    if (!variant.attachmentId) {
      return;
    }
    previewUrlValue.value = previewUrl(variant.attachmentId);
    previewTitle.value = variant.hookText || variant.titleText || t('replica.preview');
    previewVisible.value = true;
  }

  async function handleDownload(variant: AiReplicaVariant) {
    if (!variant.attachmentId) {
      return;
    }
    try {
      const res = await downloadAttachment(variant.attachmentId);
      const url = URL.createObjectURL(new Blob([res], { type: 'application/octet-stream' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = `replica-variant-${variant.variantNo ?? 0}.mp4`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  function handleDeleteTask(task: AiReplicaTask) {
    if (!task.id) {
      return;
    }
    dialog.warning({
      title: t('common.confirmDelete'),
      content: t('replica.deleteTaskConfirm'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiReplicaTask(task.id);
          Message.success(t('common.operationSuccess'));
          await loadTasks();
        } catch (error) {
          Message.error((error as Error)?.message || t('common.operationFailed'));
        }
      },
    });
  }

  function handleDeleteVariant(variant: AiReplicaVariant) {
    if (!variant.id) {
      return;
    }
    dialog.warning({
      title: t('common.confirmDelete'),
      content: t('replica.deleteVariantConfirm'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiReplicaVariant(variant.id);
          Message.success(t('common.operationSuccess'));
          await loadTasks();
        } catch (error) {
          Message.error((error as Error)?.message || t('common.operationFailed'));
        }
      },
    });
  }

  onMounted(async () => {
    try {
      available.value = await getAiReplicaAvailable();
      if (available.value) {
        await loadTasks();
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
  .replica-page {
    overflow: auto;
    padding: 24px;
    height: 100%;
  }
  .replica-head {
    margin-bottom: 20px;
  }
  .replica-title {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    color: var(--text-n1, #1f2937);
  }
  .replica-desc {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--text-n2, #6b7280);
  }
  .replica-locked {
    margin-top: 60px;
  }
  .replica-create {
    display: flex;
    align-items: flex-end;
    gap: 20px;
    padding: 20px;
    border-radius: 12px;
    background: #ffffff;
    box-shadow: 0 1px 3px rgb(0 0 0 / 6%);
    flex-wrap: wrap;
  }
  .replica-create-video {
    flex: 1;
    min-width: 260px;
  }
  .replica-create-count {
    flex-shrink: 0;
  }
  .replica-label {
    display: block;
    margin-bottom: 8px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-n1, #1f2937);
  }
  .replica-tip {
    margin: 6px 0 0;
    font-size: 12px;
    line-height: 1.6;
    color: #f97316;
  }
  .replica-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-top: 20px;
  }
  .replica-empty {
    padding: 40px 0;
    border-radius: 12px;
    background: #ffffff;
  }
  .replica-task {
    padding: 16px 20px;
    border-radius: 12px;
    background: #ffffff;
    box-shadow: 0 1px 3px rgb(0 0 0 / 6%);
  }
  .replica-task-head {
    display: flex;
    align-items: center;
    gap: 14px;
  }
  .replica-task-cover {
    width: 72px;
    height: 48px;
    object-fit: cover;
    border-radius: 6px;
    background: #f3f4f6;
    flex-shrink: 0;
  }
  .replica-task-cover-empty {
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 12px;
    color: var(--text-n2, #6b7280);
  }
  .replica-task-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    flex: 1;
  }
  .replica-task-name {
    overflow: hidden;
    font-size: 15px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-n1, #1f2937);
  }
  .replica-task-sub {
    font-size: 12px;
    color: var(--text-n2, #6b7280);
  }
  .replica-task-status {
    flex-shrink: 0;
  }
  .replica-task-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }
  .replica-tag {
    padding: 2px 8px;
    font-size: 12px;
    line-height: 18px;
    border-radius: 4px;
  }
  .replica-tag-success {
    color: #16a34a;
    background: #f0fdf4;
  }
  .replica-tag-failed {
    color: #dc2626;
    background: #fef2f2;
  }
  .replica-tag-running {
    color: #f97316;
    background: #fff7ed;
  }
  .replica-task-error {
    margin: 8px 0 0;
    font-size: 12px;
    color: #dc2626;
  }
  .replica-variants {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 14px;
    padding-top: 14px;
    border-top: 1px solid #f3f4f6;
  }
  .replica-variant {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    padding: 12px 14px;
    border-radius: 10px;
    background: #fafafa;
  }
  .replica-variant-main {
    min-width: 0;
    flex: 1;
  }
  .replica-variant-head {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }
  .replica-variant-no {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-n1, #1f2937);
  }
  .replica-variant-ratio,
  .replica-variant-style {
    padding: 1px 6px;
    font-size: 12px;
    border-radius: 4px;
    color: #475569;
    background: #eef2f7;
  }
  .replica-variant-hook {
    margin: 8px 0 0;
    font-size: 14px;
    font-weight: 600;
    line-height: 1.5;
    color: var(--text-n1, #1f2937);
  }
  .replica-variant-title {
    margin: 4px 0 0;
    font-size: 13px;
    line-height: 1.5;
    color: var(--text-n2, #6b7280);
  }
  .replica-variant-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }
  .replica-preview-video {
    width: 100%;
    max-height: 70vh;
    border-radius: 8px;
    background: #000000;
  }
</style>
