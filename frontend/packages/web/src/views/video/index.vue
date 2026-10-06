<template>
  <div class="video-page flex h-full flex-col gap-[12px] overflow-auto p-[16px]">
    <!-- 生成区 -->
    <CrmCard class="video-generate-card shrink-0" :title="t('menu.aiVideo')" hide-footer auto-height>
      <div class="flex flex-col gap-[12px]">
        <div class="text-[13px] text-orange-500">{{ t('video.desc') }}</div>

        <div class="flex items-start gap-[12px]">
          <div class="flex w-[260px] shrink-0 flex-col gap-[6px]">
            <span class="text-[13px]">{{ t('video.topicLabel') }}</span>
            <n-input
              v-model:value="form.title"
              type="textarea"
              :placeholder="t('video.topicPlaceholder')"
              :autosize="{ minRows: 2, maxRows: 4 }"
            />
          </div>
          <div class="flex min-w-0 flex-1 flex-col gap-[6px]">
            <span class="text-[13px]">{{ t('video.extraLabel') }}</span>
            <n-input
              v-model:value="form.extraText"
              type="textarea"
              :placeholder="t('video.extraPlaceholder')"
              :autosize="{ minRows: 2, maxRows: 4 }"
            />
          </div>
        </div>

        <div class="flex items-end gap-[12px]">
          <div class="flex w-[200px] flex-col gap-[6px]">
            <span class="text-[13px]">{{ t('video.voiceLabel') }}</span>
            <n-select v-model:value="form.voice" :options="voiceOptions" />
          </div>
          <div class="flex w-[160px] flex-col gap-[6px]">
            <span class="text-[13px]">{{ t('video.styleLabel') }}</span>
            <n-select v-model:value="form.style" :options="styleOptions" />
          </div>
          <div class="ml-auto">
            <AiActionButton :loading="generating" :disabled="!canGenerate" @click="handleGenerate">
              {{ generating ? t('video.generating') : t('video.generate') }}
            </AiActionButton>
          </div>
        </div>

        <div v-if="hasRunning" class="flex items-center gap-[8px]">
          <ShimmerText size="14px">{{ t('video.generating') }}</ShimmerText>
        </div>
      </div>
    </CrmCard>

    <!-- 任务列表区 -->
    <CrmCard class="video-list-card min-h-0 flex-1" :title="t('video.taskList')" hide-footer>
      <n-empty v-if="!tasks.length && !loading" :description="t('video.noTask')" class="mt-[40px]" />
      <n-data-table
        v-else
        :columns="columns"
        :data="tasks"
        :row-key="(row: AiVideoTask) => row.id"
        :loading="loading"
        :bordered="false"
        size="small"
      />

      <div class="mt-[12px] text-[12px] text-orange-500">{{ t('video.compliance') }}</div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, onBeforeUnmount, onMounted, ref } from 'vue';
  import { NButton, NDataTable, NEmpty, NInput, NProgress, NSelect, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiVideoGenerateParams, AiVideoTask, AiVideoVoice } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  import {
    createAiVideoTask,
    deleteAiVideoTask,
    downloadAttachment,
    getAiVideoTasks,
    getAiVideoVoices,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  import type { DataTableColumns } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const form = ref<AiVideoGenerateParams>({
    title: '',
    extraText: '',
    voice: 'Cherry',
    style: '口播',
  });
  const generating = ref(false);
  const tasks = ref<AiVideoTask[]>([]);
  const loading = ref(false);
  const voices = ref<AiVideoVoice[]>([]);

  const IN_PROGRESS = ['PENDING', 'SCRIPTING', 'TTS', 'SYNTH'];

  // 音色列表跟随「模型设置」里 admin 配置的配音模型，换服务商后前端无需改动
  const voiceOptions = computed(() => voices.value.map((v) => ({ label: v.label, value: v.value })));

  const styleOptions = computed(() => [
    { label: t('video.style.koushuo'), value: '口播' },
    { label: t('video.style.cuxiao'), value: '促销' },
  ]);

  const canGenerate = computed(() => !!form.value.title.trim());

  const hasRunning = computed(() => tasks.value.some((task) => IN_PROGRESS.includes(task.status)));

  function isInProgress(task: AiVideoTask): boolean {
    return IN_PROGRESS.includes(task.status);
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
      SCRIPTING: '#2563eb',
      TTS: '#2563eb',
      SYNTH: '#2563eb',
      DONE: '#18a058',
      FAILED: '#e5484d',
    };
    const color = map[status] || '#8a94a6';
    return `padding:2px 8px;border-radius:4px;font-size:12px;color:${color};background:${color}1a;`;
  }

  function renderStatus(status: string) {
    const key = `video.status.${status}`;
    const text = t(key);
    return h('span', { style: statusStyle(status) }, text === key ? status : text);
  }

  async function loadTasks() {
    try {
      tasks.value = await getAiVideoTasks();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  async function loadVoices() {
    try {
      const list = await getAiVideoVoices();
      voices.value = list ?? [];
      if (voices.value.length && !voices.value.some((v) => v.value === form.value.voice)) {
        form.value.voice = voices.value[0].value;
      }
    } catch {
      // 音色拉取失败忽略，保留默认值
    }
  }

  let pollTimer: number | null = null;

  function stopPolling() {
    if (pollTimer !== null) {
      window.clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  function startPolling() {
    stopPolling();
    pollTimer = window.setInterval(async () => {
      try {
        await loadTasks();
        if (!hasRunning.value) {
          stopPolling();
        }
      } catch {
        // 轮询失败忽略，等待下一轮重试
      }
    }, 3000);
  }

  async function handleGenerate() {
    if (!canGenerate.value) {
      Message.warning(t('video.topicPlaceholder'));
      return;
    }
    generating.value = true;
    try {
      await createAiVideoTask({
        title: form.value.title.trim(),
        extraText: form.value.extraText?.trim(),
        voice: form.value.voice,
        style: form.value.style,
      });
      Message.success(t('video.generateSuccess'));
      form.value.title = '';
      form.value.extraText = '';
      await loadTasks();
      startPolling();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      generating.value = false;
    }
  }

  async function handleDownload(row: AiVideoTask) {
    if (!row.attachmentId) {
      return;
    }
    try {
      const res = await downloadAttachment(row.attachmentId);
      const url = URL.createObjectURL(new Blob([res], { type: 'application/octet-stream' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = `${row.title}.mp4`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  function handleDelete(row: AiVideoTask) {
    openModal({
      type: 'error',
      title: t('video.delete'),
      content: t('video.deleteConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiVideoTask(row.id);
          Message.success(t('common.deleteSuccess'));
          await loadTasks();
        } catch (error) {
          Message.error((error as Error)?.message || t('common.operationFailed'));
        }
      },
    });
  }

  const columns = computed<DataTableColumns<AiVideoTask>>(() => [
    {
      title: t('video.colTitle'),
      key: 'title',
      minWidth: 240,
      render: (row) =>
        h('div', { class: 'flex flex-col gap-[2px]' }, [
          h('span', { class: 'text-[13px]' }, row.title),
          row.status === 'FAILED' && row.errorMsg
            ? h('span', { class: 'text-[12px] text-[#e5484d]' }, row.errorMsg)
            : null,
        ]),
    },
    {
      title: t('video.colStatus'),
      key: 'status',
      width: 110,
      render: (row) => renderStatus(row.status),
    },
    {
      title: t('video.colProgress'),
      key: 'progress',
      width: 140,
      render: (row) =>
        h(NProgress, {
          type: 'line',
          percentage: row.status === 'DONE' ? 100 : row.progress ?? 0,
          processing: isInProgress(row),
          indicatorPlacement: 'inside',
          height: 16,
        }),
    },
    {
      title: t('video.colTime'),
      key: 'createTime',
      width: 150,
      render: (row) => h('span', { class: 'text-[12px]' }, formatTime(row.createTime)),
    },
    {
      title: t('video.colAction'),
      key: 'action',
      width: 140,
      render: (row) =>
        h('div', { class: 'flex items-center gap-[8px]' }, [
          row.status === 'DONE' && row.attachmentId
            ? h(
                NButton,
                { size: 'tiny', text: true, type: 'primary', onClick: () => handleDownload(row) },
                { default: () => t('video.download') }
              )
            : null,
          h(
            NButton,
            { size: 'tiny', text: true, type: 'error', onClick: () => handleDelete(row) },
            { default: () => t('video.delete') }
          ),
        ]),
    },
  ]);

  onMounted(async () => {
    loading.value = true;
    try {
      await loadVoices();
      await loadTasks();
      if (hasRunning.value) {
        startPolling();
      }
    } finally {
      loading.value = false;
    }
  });

  onBeforeUnmount(stopPolling);
</script>
