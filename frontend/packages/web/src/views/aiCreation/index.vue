<template>
  <div class="pf-page">
    <!-- 顶部品牌区 -->
    <section class="pf-hero">
      <div class="pf-hero__text">
        <h1 class="pf-hero__title">{{ t('menu.aiCreation') }}</h1>
        <p class="pf-hero__subtitle">{{ t('aiCreation.subtitle') }}</p>
      </div>
      <span v-if="available === false" class="pf-hero__badge pf-hero__badge--warn">
        {{ t('aiCreation.upgradeHint') }}
      </span>
    </section>

    <!-- 工具台 -->
    <section class="pf-grid">
      <button
        v-for="tool in tools"
        :key="tool.key"
        class="pf-tool"
        :class="{ 'pf-tool--active': activeKey === tool.key, 'pf-tool--disabled': !tool.wired }"
        :style="toolCardStyle(tool)"
        type="button"
        @click="selectTool(tool)"
      >
        <span class="pf-tool__icon" :style="{ background: tool.soft, color: tool.color }">
          <CrmIcon :type="tool.icon" :size="22" />
        </span>
        <span class="pf-tool__name">{{ tool.name }}</span>
        <span class="pf-tool__desc">{{ tool.desc }}</span>
        <span v-if="!tool.wired" class="pf-tool__badge">{{ t('aiCreation.comingSoon') }}</span>
      </button>
    </section>

    <!-- 工作台：选择工具后出现 -->
    <section v-if="activeTool" class="pf-workbench">
      <div class="pf-workbench__head">
        <div class="flex items-center gap-[10px]">
          <span class="pf-workbench__icon" :style="{ background: activeTool.soft, color: activeTool.color }">
            <CrmIcon :type="activeTool.icon" :size="18" />
          </span>
          <h2 class="pf-workbench__title">{{ activeTool.name }}</h2>
        </div>
        <button class="pf-back" type="button" @click="closeTool">
          {{ t('aiCreation.back') }}
        </button>
      </div>

      <!-- 文案改写 / 爆款标题 / 口播脚本 -->
      <div v-if="isTextTool" class="pf-workbench__body">
        <label class="pf-field">
          <span class="pf-field__label">{{ t('aiCreation.text.content') }}</span>
          <n-input
            v-model:value="textContent"
            type="textarea"
            :autosize="{ minRows: 4, maxRows: 8 }"
            :placeholder="t(`aiCreation.text.${activeKey}.placeholder`)"
          />
        </label>
        <label class="pf-field">
          <span class="pf-field__label">{{ t('aiCreation.text.option') }}</span>
          <n-input v-model:value="textOption" :placeholder="t('aiCreation.text.optionPlaceholder')" />
        </label>

        <div class="pf-actions">
          <button class="pf-btn" type="button" :disabled="!canGenerateText" @click="handleGenerateText">
            {{ textGenerating ? t('aiCreation.generating') : t('aiCreation.generate') }}
          </button>
        </div>

        <div v-if="textGenerating" class="pf-result pf-result--thinking">
          <ShimmerText size="15px">{{ t('aiCreation.generating') }}</ShimmerText>
          <div v-if="revealedText" class="pf-result__text">{{ revealedText }}</div>
        </div>
        <div v-else-if="textResult" class="pf-result">
          <div class="pf-result__head">
            <span class="pf-result__label">{{ t('aiCreation.result') }}</span>
            <button class="pf-btn pf-btn--ghost" :class="{ 'pf-btn--copied': copied }" type="button" @click="copyText">
              {{ copied ? t('aiCreation.copied') : t('aiCreation.copy') }}
            </button>
          </div>
          <div class="pf-result__text">{{ textResult }}</div>
        </div>
      </div>

      <!-- 文生图 -->
      <div v-else-if="activeKey === 'image'" class="pf-workbench__body">
        <label class="pf-field">
          <span class="pf-field__label">{{ t('aiCreation.image.prompt') }}</span>
          <n-input
            v-model:value="imagePrompt"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            :placeholder="t('aiCreation.image.promptPlaceholder')"
          />
        </label>
        <div class="pf-field pf-field--row">
          <span class="pf-field__label">{{ t('aiCreation.image.size') }}</span>
          <n-select v-model:value="imageSize" :options="imageSizeOptions" class="!w-[220px]" />
        </div>

        <div class="pf-actions">
          <button
            class="pf-btn"
            type="button"
            :disabled="!canGenerateImage || imageGenerating"
            @click="handleGenerateImage"
          >
            {{ imageGenerating ? t('aiCreation.image.generating') : t('aiCreation.image.generate') }}
          </button>
        </div>

        <div v-if="imageGenerating" class="pf-result pf-result--thinking">
          <ShimmerText size="15px">{{ t('aiCreation.image.generating') }}</ShimmerText>
          <n-progress
            type="line"
            :percentage="imageProgress"
            :processing="true"
            indicator-placement="inside"
            class="mt-[12px]"
          />
        </div>
        <div v-else-if="imageResultUrl" class="pf-result">
          <img :src="imageResultUrl" :alt="t('aiCreation.image.prompt')" class="pf-result__img" />
        </div>
      </div>

      <!-- 视频转格式 -->
      <div v-else-if="activeKey === 'transcode'" class="pf-workbench__body">
        <div class="pf-field pf-field--row">
          <span class="pf-field__label">{{ t('aiCreation.transcode.file') }}</span>
          <n-upload :default-upload="false" :max="1" accept="video/*" @change="handleTranscodeFileChange">
            <button class="pf-btn pf-btn--ghost" type="button">
              {{ transcodeFile ? transcodeFile.name : t('aiCreation.transcode.upload') }}
            </button>
          </n-upload>
        </div>
        <div class="pf-field pf-field--row">
          <span class="pf-field__label">{{ t('aiCreation.transcode.format') }}</span>
          <n-select v-model:value="transcodeFormat" :options="transcodeFormatOptions" class="!w-[220px]" />
        </div>

        <div class="pf-actions">
          <button
            class="pf-btn"
            type="button"
            :disabled="!transcodeAttachmentId || transcodeGenerating"
            @click="handleGenerateTranscode"
          >
            {{ transcodeGenerating ? t('aiCreation.transcode.generating') : t('aiCreation.transcode.generate') }}
          </button>
        </div>

        <div v-if="transcodeGenerating" class="pf-result pf-result--thinking">
          <ShimmerText size="15px">{{ t('aiCreation.transcode.generating') }}</ShimmerText>
          <n-progress
            type="line"
            :percentage="transcodeProgress"
            :processing="true"
            indicator-placement="inside"
            class="mt-[12px]"
          />
        </div>
      </div>
    </section>

    <!-- 空态：未选择工具 -->
    <section v-else class="pf-empty">
      <span class="pf-empty__icon">
        <CrmIcon type="iconicon_crmbot" :size="28" />
      </span>
      <p>{{ t('aiCreation.pickHint') }}</p>
    </section>

    <!-- 生成记录 -->
    <section class="pf-workbench">
      <div class="pf-workbench__head">
        <h2 class="pf-workbench__title">{{ t('aiCreation.taskList') }}</h2>
      </div>
      <div class="pf-workbench__body">
        <n-empty v-if="!tasks.length && !loading" :description="t('aiCreation.noTask')" class="py-[32px]" />
        <n-data-table
          v-else
          :columns="columns"
          :data="tasks"
          :row-key="(row: AiToolTask) => row.id"
          :loading="loading"
          :bordered="false"
          remote
          :pagination="pagination"
          size="small"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, onBeforeUnmount, onMounted, ref } from 'vue';
  import { NButton, NDataTable, NEmpty, NInput, NProgress, NSelect, NUpload, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiToolTask } from '@lib/shared/models/ai';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';

  import {
    createAiToolImage,
    createAiToolTranscode,
    deleteAiToolTask,
    downloadAttachment,
    getAiToolAvailable,
    getAiToolTasksPage,
    streamAiToolText,
    uploadTempAttachment,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useTypewriter from '@/hooks/useTypewriter';

  import type { DataTableColumns, UploadFileInfo } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  // 工具定义（key 同时作为 i18n 命名空间，wired=false 为「即将上线」占位）
  // color 为图标主色、soft 为图标底色，取亮色系避免工具台显得沉闷
  const TOOL_DEFS: { key: string; icon: string; wired: boolean; color: string; soft: string }[] = [
    { key: 'image', icon: 'iconicon_image', wired: true, color: '#7c3aed', soft: '#ede9fe' },
    { key: 'rewrite', icon: 'iconicon_edit1', wired: true, color: '#0891b2', soft: '#cffafe' },
    { key: 'title', icon: 'iconicon_hashtag', wired: true, color: '#ea580c', soft: '#ffedd5' },
    { key: 'script', icon: 'iconicon_sound', wired: true, color: '#db2777', soft: '#fce7f3' },
    { key: 'transcode', icon: 'iconicon_play', wired: true, color: '#2563eb', soft: '#dbeafe' },
    { key: 'cutout', icon: 'iconicon_scan', wired: false, color: '#16a34a', soft: '#dcfce7' },
    { key: 'upscale', icon: 'iconicon_zoom', wired: false, color: '#d97706', soft: '#fef3c7' },
    { key: 'outpaint', icon: 'iconicon_aiming', wired: false, color: '#0d9488', soft: '#ccfbf1' },
    { key: 'dewatermark', icon: 'iconicon_picture', wired: false, color: '#e11d48', soft: '#ffe4e6' },
  ];

  const tools = computed(() =>
    TOOL_DEFS.map((tool) => ({
      ...tool,
      name: t(`aiCreation.tool.${tool.key}`),
      desc: t(`aiCreation.tool.${tool.key}Desc`),
    }))
  );

  const activeKey = ref('');
  const available = ref<boolean | null>(null);

  // 选中态边框用亮色 lime，外围光晕用半透明 lime（与上一版一致）
  function toolCardStyle(tool: { key: string }): Record<string, string> {
    if (activeKey.value !== tool.key) {
      return {};
    }
    return { borderColor: '#b6ff00', boxShadow: '0 0 0 3px rgb(182 255 0 / 35%)' };
  }

  const activeTool = computed(() => tools.value.find((tool) => tool.key === activeKey.value));
  const isTextTool = computed(
    () => activeKey.value === 'rewrite' || activeKey.value === 'title' || activeKey.value === 'script'
  );

  // ==================== 文案类工具 ====================
  const textContent = ref('');
  const textOption = ref('');
  const textGenerating = ref(false);
  const textResult = ref('');
  const copied = ref(false);
  let copyResetTimer: number | null = null;
  const { text: revealedText, feed: feedText, finish: finishText, reset: resetText } = useTypewriter();

  const canGenerateText = computed(() => textContent.value.trim() !== '' && !textGenerating.value);

  function textType(): 'REWRITE' | 'TITLE' | 'SCRIPT' {
    if (activeKey.value === 'title') {
      return 'TITLE';
    }
    if (activeKey.value === 'script') {
      return 'SCRIPT';
    }
    return 'REWRITE';
  }

  async function handleGenerateText() {
    if (!canGenerateText.value) {
      return;
    }
    textGenerating.value = true;
    textResult.value = '';
    copied.value = false;
    resetText();
    let full = '';

    try {
      const stream = streamAiToolText({
        type: textType(),
        content: textContent.value.trim(),
        option: textOption.value.trim() || undefined,
      });

      // eslint-disable-next-line no-restricted-syntax -- 原生 async generator，无需 regenerator-runtime
      for await (const event of stream) {
        if (event.type === 'chunk') {
          full += event.content || '';
          feedText(event.content || '');
        } else if (event.type === 'error') {
          throw new Error(event.errorMessage || t('common.operationFailed'));
        }
      }
      await finishText();
      textResult.value = full.trim();
    } catch (error) {
      resetText();
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      textGenerating.value = false;
    }
  }

  async function copyText() {
    const content = textResult.value;
    if (!content) {
      return;
    }
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(content);
      } else {
        // 非安全上下文（http 非 localhost）下 clipboard API 不可用，退回 execCommand
        const textarea = document.createElement('textarea');
        textarea.value = content;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
      }
      copied.value = true;
      Message.success(t('aiCreation.copied'));
      if (copyResetTimer !== null) {
        window.clearTimeout(copyResetTimer);
      }
      copyResetTimer = window.setTimeout(() => {
        copied.value = false;
      }, 2000);
    } catch {
      Message.error(t('common.operationFailed'));
    }
  }

  // ==================== 文生图（状态） ====================
  const imagePrompt = ref('');
  const imageSize = ref('1080x1920');
  const imageGenerating = ref(false);
  const imageProgress = ref(0);
  const imageResultUrl = ref('');
  const activeImageTaskId = ref('');

  const imageSizeOptions = computed(() => [
    { label: t('aiCreation.image.size.vertical'), value: '1080x1920' },
    { label: t('aiCreation.image.size.square'), value: '1024x1024' },
    { label: t('aiCreation.image.size.horizontal'), value: '1920x1080' },
  ]);

  const canGenerateImage = computed(() => imagePrompt.value.trim() !== '' && !imageGenerating.value);

  // ==================== 视频转格式（状态） ====================
  const transcodeFile = ref<File | null>(null);
  const transcodeAttachmentId = ref('');
  const transcodeFormat = ref('mp4');
  const transcodeGenerating = ref(false);
  const transcodeProgress = ref(0);
  const activeTranscodeTaskId = ref('');

  const transcodeFormatOptions = computed(() =>
    ['mp4', 'mov', 'gif', 'webm'].map((value) => ({ label: value.toUpperCase(), value }))
  );

  // ==================== 任务记录（状态 + 轮询辅助） ====================
  const tasks = ref<AiToolTask[]>([]);
  const loading = ref(false);
  const pageNum = ref(1);
  const pageSize = ref(10);
  const total = ref(0);

  const IN_PROGRESS = ['PENDING', 'PROCESSING'];

  const hasRunning = computed(() => tasks.value.some((task) => IN_PROGRESS.includes(task.status)));

  let pollTimer: number | null = null;

  function revokeObjectUrl(url: string) {
    if (url) {
      URL.revokeObjectURL(url);
    }
  }

  async function loadImagePreview(attachmentId: string) {
    try {
      const res = await downloadAttachment(attachmentId);
      const url = URL.createObjectURL(res instanceof Blob ? res : new Blob([res]));
      revokeObjectUrl(imageResultUrl.value);
      imageResultUrl.value = url;
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  async function loadTasks() {
    try {
      const res = await getAiToolTasksPage({ current: pageNum.value, pageSize: pageSize.value });
      tasks.value = res?.list ?? [];
      total.value = res?.total ?? 0;
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  function handlePageChange(page: number) {
    pageNum.value = page;
    loadTasks();
  }

  function handlePageSizeChange(size: number) {
    pageSize.value = size;
    pageNum.value = 1;
    loadTasks();
  }

  const pagination = computed(() => ({
    page: pageNum.value,
    pageSize: pageSize.value,
    itemCount: total.value,
    showSizePicker: true,
    pageSizes: [10, 20, 50],
    onChange: handlePageChange,
    onUpdatePageSize: handlePageSizeChange,
  }));

  function stopPolling() {
    if (pollTimer !== null) {
      window.clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  // 把轮询到的当前任务进度同步到工作台，完成时拉取结果预览
  function syncActiveTaskState() {
    const imageTask = tasks.value.find((task) => task.id === activeImageTaskId.value);
    if (imageTask) {
      imageProgress.value = imageTask.status === 'DONE' ? 100 : imageTask.progress ?? 0;
      if (imageTask.status === 'DONE' && imageTask.attachmentId) {
        loadImagePreview(imageTask.attachmentId);
        imageGenerating.value = false;
        activeImageTaskId.value = '';
      } else if (imageTask.status === 'FAILED') {
        imageGenerating.value = false;
        activeImageTaskId.value = '';
        Message.error(imageTask.errorMsg || t('common.operationFailed'));
      }
    }

    const transcodeTask = tasks.value.find((task) => task.id === activeTranscodeTaskId.value);
    if (transcodeTask) {
      transcodeProgress.value = transcodeTask.status === 'DONE' ? 100 : transcodeTask.progress ?? 0;
      if (transcodeTask.status === 'DONE') {
        transcodeGenerating.value = false;
        activeTranscodeTaskId.value = '';
      } else if (transcodeTask.status === 'FAILED') {
        transcodeGenerating.value = false;
        activeTranscodeTaskId.value = '';
        Message.error(transcodeTask.errorMsg || t('common.operationFailed'));
      }
    }
  }

  function startPolling() {
    stopPolling();
    pollTimer = window.setInterval(async () => {
      try {
        await loadTasks();
        syncActiveTaskState();
        if (!hasRunning.value) {
          stopPolling();
        }
      } catch {
        // 轮询失败忽略，等待下一轮
      }
    }, 3000);
  }

  // ==================== 文生图（动作） ====================
  async function handleGenerateImage() {
    if (!canGenerateImage.value) {
      return;
    }
    imageGenerating.value = true;
    imageProgress.value = 0;
    revokeObjectUrl(imageResultUrl.value);
    imageResultUrl.value = '';

    try {
      const task = await createAiToolImage({ prompt: imagePrompt.value.trim(), size: imageSize.value });
      activeImageTaskId.value = task.id;
      pageNum.value = 1;
      await loadTasks();
      startPolling();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
      imageGenerating.value = false;
    }
  }

  // ==================== 视频转格式（动作） ====================
  async function handleTranscodeFileChange(data: { file: UploadFileInfo }) {
    const file = data.file?.file;
    if (!file) {
      return;
    }
    try {
      const res = await uploadTempAttachment(file);
      const id = Array.isArray(res?.data) ? res.data[0] : '';
      if (!id) {
        throw new Error(t('common.operationFailed'));
      }
      transcodeFile.value = file;
      transcodeAttachmentId.value = id;
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  async function handleGenerateTranscode() {
    if (!transcodeAttachmentId.value || transcodeGenerating.value) {
      return;
    }
    transcodeGenerating.value = true;
    transcodeProgress.value = 0;

    try {
      const task = await createAiToolTranscode({
        sourceAttachmentId: transcodeAttachmentId.value,
        sourceName: transcodeFile.value?.name,
        format: transcodeFormat.value,
      });
      activeTranscodeTaskId.value = task.id;
      pageNum.value = 1;
      await loadTasks();
      startPolling();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
      transcodeGenerating.value = false;
    }
  }

  function selectTool(tool: { key: string; wired: boolean }) {
    if (!tool.wired) {
      Message.info(t('aiCreation.comingSoon'));
      return;
    }
    if (activeKey.value === tool.key) {
      return;
    }
    activeKey.value = tool.key;
    // 切换工具时清空文案结果，避免串台
    textResult.value = '';
    copied.value = false;
    resetText();
  }

  function closeTool() {
    activeKey.value = '';
    textResult.value = '';
    copied.value = false;
    resetText();
  }

  async function handleDownload(row: AiToolTask) {
    if (!row.attachmentId) {
      return;
    }
    try {
      const res = await downloadAttachment(row.attachmentId);
      const url = URL.createObjectURL(res instanceof Blob ? res : new Blob([res]));
      const a = document.createElement('a');
      a.href = url;
      a.download = row.name || `output-${row.id}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  function handleDelete(row: AiToolTask) {
    openModal({
      type: 'error',
      title: t('aiCreation.delete'),
      content: t('aiCreation.deleteConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiToolTask(row.id);
          Message.success(t('common.deleteSuccess'));
          await loadTasks();
        } catch (error) {
          Message.error((error as Error)?.message || t('common.operationFailed'));
        }
      },
    });
  }

  function formatTime(ts?: number): string {
    if (!ts) {
      return '-';
    }
    const d = dayjs(ts);
    return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : '-';
  }

  function statusStyle(status: string): string {
    const map: Record<string, string> = {
      PENDING: '#f97316',
      PROCESSING: '#2563eb',
      DONE: '#18a058',
      FAILED: '#e5484d',
    };
    const color = map[status] || '#8a94a6';
    return `padding:2px 8px;border-radius:9999px;font-size:12px;color:${color};background:${color}1a;`;
  }

  function renderStatus(status: string) {
    const key = `aiCreation.status.${status}`;
    const text = t(key);
    return h('span', { style: statusStyle(status) }, text === key ? status : text);
  }

  const columns = computed<DataTableColumns<AiToolTask>>(() => [
    {
      title: t('aiCreation.colName'),
      key: 'name',
      minWidth: 220,
      render: (row) =>
        h('div', { class: 'flex flex-col gap-[2px]' }, [
          h('span', { class: 'text-[13px]' }, row.name || t(`aiCreation.type.${row.type}`)),
          row.status === 'FAILED' && row.errorMsg
            ? h('span', { class: 'text-[12px] text-[#e5484d]' }, row.errorMsg)
            : null,
        ]),
    },
    {
      title: t('aiCreation.colType'),
      key: 'type',
      width: 110,
      render: (row) => h('span', { class: 'text-[12px]' }, t(`aiCreation.type.${row.type}`)),
    },
    {
      title: t('aiCreation.colStatus'),
      key: 'status',
      width: 110,
      render: (row) => renderStatus(row.status),
    },
    {
      title: t('aiCreation.colProgress'),
      key: 'progress',
      width: 140,
      render: (row) =>
        h(NProgress, {
          type: 'line',
          percentage: row.status === 'DONE' ? 100 : row.progress ?? 0,
          processing: IN_PROGRESS.includes(row.status),
          indicatorPlacement: 'inside',
          height: 16,
        }),
    },
    {
      title: t('aiCreation.colTime'),
      key: 'createTime',
      width: 150,
      render: (row) => h('span', { class: 'text-[12px]' }, formatTime(row.createTime)),
    },
    {
      title: t('aiCreation.colAction'),
      key: 'action',
      width: 140,
      render: (row) =>
        h('div', { class: 'flex items-center gap-[8px]' }, [
          row.status === 'DONE' && row.attachmentId
            ? h(
                NButton,
                { size: 'tiny', text: true, type: 'primary', onClick: () => handleDownload(row) },
                { default: () => t('aiCreation.download') }
              )
            : null,
          h(
            NButton,
            { size: 'tiny', text: true, type: 'error', onClick: () => handleDelete(row) },
            { default: () => t('aiCreation.delete') }
          ),
        ]),
    },
  ]);

  onMounted(async () => {
    loading.value = true;
    try {
      try {
        available.value = await getAiToolAvailable();
      } catch {
        available.value = true; // 拉取失败交给后端门控
      }
      await loadTasks();
      if (hasRunning.value) {
        startPolling();
      }
    } finally {
      loading.value = false;
    }
  });

  onBeforeUnmount(() => {
    stopPolling();
    revokeObjectUrl(imageResultUrl.value);
  });
</script>

<style lang="less" scoped>
  .pf-page {
    --pf-lime: #b6ff00;
    --pf-lime-soft: #eefcc8;
    --pf-ink: #111318;
    --pf-muted: #6b7280;
    --pf-line: rgb(17 19 24 / 8%);
    --pf-shadow: 0 8px 28px rgb(17 19 24 / 6%);

    overflow: auto;
    padding: 24px;
    height: 100%;
    background: #f7f8fa;
  }

  // ---------- 品牌区 ----------
  .pf-hero {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
    gap: 16px;
    &__title {
      margin: 0;
      font-size: 26px;
      line-height: 34px;
      font-weight: 800;
      color: var(--pf-ink);
      letter-spacing: -0.5px;
    }
    &__subtitle {
      margin: 6px 0 0;
      font-size: 14px;
      color: var(--pf-muted);
    }
    &__badge {
      padding: 6px 14px;
      font-size: 12px;
      border-radius: 9999px;
      white-space: nowrap;
      &--warn {
        color: #f97316;
        background: rgb(249 115 22 / 10%);
      }
    }
  }

  // ---------- 工具台 ----------
  .pf-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 14px;
    margin-bottom: 20px;
  }
  .pf-tool {
    position: relative;
    display: flex;
    align-items: center;
    padding: 18px;
    border: 1px solid var(--pf-line);
    border-radius: 14px;
    text-align: center;
    background: #ffffff;
    box-shadow: var(--pf-shadow);
    transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
    flex-direction: column;
    gap: 8px;
    cursor: pointer;
    &:hover:not(.pf-tool--disabled) {
      transform: translateY(-2px);
      border-color: rgb(17 19 24 / 14%);
      box-shadow: 0 14px 34px rgb(17 19 24 / 10%);
    }
    &--disabled {
      cursor: not-allowed;
      opacity: 0.62;
    }
    &__icon {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 40px;
      height: 40px;
      border-radius: 12px;
    }
    &__name {
      font-size: 15px;
      font-weight: 700;
      color: var(--pf-ink);
    }
    &__desc {
      font-size: 12px;
      line-height: 1.5;
      color: var(--pf-muted);
    }
    &__badge {
      position: absolute;
      top: 12px;
      right: 12px;
      padding: 2px 8px;
      font-size: 11px;
      border-radius: 9999px;
      color: #8a94a6;
      background: rgb(138 148 166 / 14%);
    }
  }

  // ---------- 工作台 ----------
  .pf-workbench {
    margin-bottom: 20px;
    padding: 20px;
    border: 1px solid var(--pf-line);
    border-radius: 14px;
    background: #ffffff;
    box-shadow: var(--pf-shadow);
    &__head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      gap: 12px;
    }
    &__icon {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 32px;
      height: 32px;
      border-radius: 10px;
    }
    &__title {
      margin: 0;
      font-size: 16px;
      font-weight: 700;
      color: var(--pf-ink);
    }
    &__body {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
  }
  .pf-back {
    padding: 6px 14px;
    font-size: 13px;
    border: 1px solid var(--pf-line);
    border-radius: 9999px;
    color: var(--pf-ink);
    background: transparent;
    transition: background 0.15s ease;
    cursor: pointer;
    &:hover {
      background: #f3f4f6;
    }
  }

  // ---------- 表单与按钮 ----------
  .pf-field {
    display: flex;
    flex-direction: column;
    gap: 6px;
    &--row {
      flex-direction: row;
      align-items: center;
      gap: 12px;
    }
    &__label {
      min-width: 88px;
      font-size: 13px;
      color: var(--pf-ink);
    }
  }
  .pf-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .pf-btn {
    padding: 9px 22px;
    font-size: 14px;
    font-weight: 700;
    border: none;
    border-radius: 9999px;
    color: var(--pf-ink);
    background: var(--pf-lime);
    transition: filter 0.15s ease, transform 0.15s ease;
    cursor: pointer;
    &:hover:not(:disabled) {
      filter: brightness(0.96);
      transform: translateY(-1px);
    }
    &:disabled {
      cursor: not-allowed;
      opacity: 0.45;
    }
    &--ghost {
      padding: 5px 14px;
      font-size: 13px;
      border: 1px solid var(--pf-line);
      background: transparent;
      &:hover:not(:disabled) {
        background: #f3f4f6;
        transform: none;
      }
    }
    &--copied {
      border-color: #18a058;
      color: #18a058;
      background: rgb(24 160 88 / 8%);
    }
  }

  // ---------- 结果区 ----------
  .pf-result {
    padding: 16px;
    border: 1px solid var(--pf-line);
    border-radius: 12px;
    background: #fafafa;
    &--thinking {
      display: flex;
      flex-direction: column;
      gap: 10px;
      background: #fbfcf7;
    }
    &__head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
    }
    &__label {
      font-size: 12px;
      color: var(--pf-muted);
    }
    &__text {
      font-size: 13px;
      white-space: pre-wrap;
      color: var(--pf-ink);
      line-height: 1.7;
    }
    &__img {
      max-width: 320px;
      border-radius: 10px;
    }
  }

  // ---------- 空态 ----------
  .pf-empty {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 56px 24px;
    border: 1px dashed var(--pf-line);
    border-radius: 14px;
    color: var(--pf-muted);
    flex-direction: column;
    gap: 12px;
    &__icon {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 56px;
      height: 56px;
      border-radius: 16px;
      color: var(--pf-ink);
      background: var(--pf-lime-soft);
    }
  }
</style>
