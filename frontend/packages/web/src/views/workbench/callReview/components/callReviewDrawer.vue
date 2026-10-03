<template>
  <CrmDrawer v-model:show="visible" :title="t('workbench.callReview.detail')" :footer="false" width="560px">
    <n-spin :show="loading" class="h-full">
      <div v-if="detail" class="flex flex-col gap-[16px] pb-[24px]">
        <!-- 通话信息 -->
        <div class="rounded-[6px] border border-[var(--text-n8)] p-[16px]">
          <div class="mb-[12px] flex items-center justify-between">
            <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('workbench.callReview.title') }}</span>
            <CrmTag size="small" theme="light" :type="statusMeta.type" tooltip-disabled>
              {{ statusMeta.label }}
            </CrmTag>
          </div>
          <div class="grid grid-cols-2 gap-x-[16px] gap-y-[10px] text-[13px]">
            <InfoItem :label="t('workbench.callReview.caller')" :value="detail.caller || '-'" />
            <InfoItem :label="t('workbench.callReview.callee')" :value="detail.callee || '-'" />
            <InfoItem :label="t('workbench.callReview.customerPhone')" :value="detail.customerPhone || '-'" />
            <InfoItem :label="t('workbench.callReview.customer')" :value="detail.customerName || '-'" />
            <InfoItem :label="t('workbench.callReview.callTime')" :value="formatTime(detail.callTime)" />
            <InfoItem :label="t('workbench.callReview.duration')" :value="formatDuration(detail.duration)" />
            <div v-if="detail.supplier" class="col-span-2">
              <InfoItem :label="t('workbench.callReview.config.title')" :value="detail.supplier" />
            </div>
          </div>
          <div v-if="detail.recordUrl" class="mt-[8px] break-all text-[12px] text-[var(--text-n3)]">
            {{ t('workbench.callReview.recordUrl') }}：
            <a :href="detail.recordUrl" target="_blank" rel="noopener noreferrer" class="text-orange-500">
              {{ detail.recordUrl }}
            </a>
          </div>
        </div>

        <!-- 失败原因 -->
        <div v-if="detail.status === 'FAILED' && detail.errorMsg" class="text-[13px] text-[var(--error)]">
          {{ t('workbench.callReview.errorMsg') }}：{{ detail.errorMsg }}
        </div>

        <!-- 语音转写 -->
        <div>
          <div class="mb-[8px] text-[13px] font-semibold text-[var(--text-n1)]">
            {{ t('workbench.callReview.transcript') }}
          </div>
          <div
            v-if="detail.transcript"
            class="max-h-[220px] overflow-y-auto whitespace-pre-wrap rounded-[6px] bg-[var(--fill-2)] p-[12px] text-[13px] leading-[1.6] text-[var(--text-n2)]"
          >
            {{ detail.transcript }}
          </div>
          <div v-else class="text-[13px] text-[var(--text-n4)]">{{ t('workbench.callReview.emptyTranscript') }}</div>
        </div>

        <!-- 复盘结果 -->
        <div v-if="detail.review">
          <div class="mb-[8px] flex items-center justify-between">
            <span class="text-[13px] font-semibold text-[var(--text-n1)]">
              {{ t('workbench.callReview.reviewResult') }}
            </span>
            <n-button size="small" type="primary" @click="handleToFollow">
              {{ t('workbench.callReview.toFollow') }}
            </n-button>
          </div>
          <AdvisorResult :result="detail.review" />
        </div>

        <!-- 失败重试 -->
        <div v-if="detail.status === 'FAILED'" class="flex justify-end">
          <n-button size="small" type="error" ghost :loading="retrying" @click="handleRetry">
            {{ t('workbench.callReview.retry') }}
          </n-button>
        </div>
      </div>
    </n-spin>

    <CrmFormCreateDrawer
      v-model:visible="followVisible"
      :form-key="FormDesignKeyEnum.FOLLOW_RECORD"
      :initial-values="followInitialValues"
      @saved="handleFollowSaved"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { computed, defineComponent, h, onBeforeUnmount, ref, watch } from 'vue';
  import { NButton, NSpin, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { SalesAdvisorAnalyzeResult } from '@lib/shared/models/ai';
  import type { CallReviewDetailResponse, CallReviewStatus } from '@lib/shared/models/callReview';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import AdvisorResult from '@/views/workbench/smart/components/AdvisorResult.vue';

  import { getCallReviewDetail, retryCallReview } from '@/api/modules';

  const InfoItem = defineComponent({
    props: {
      label: { type: String, required: true },
      value: { type: String, required: true },
    },
    setup(props) {
      return () =>
        h('div', { class: 'flex min-w-0 gap-[8px]' }, [
          h('span', { class: 'shrink-0 text-[var(--text-n3)]' }, props.label),
          h('span', { class: 'min-w-0 flex-1 break-all text-[var(--text-n2)]' }, props.value),
        ]);
    },
  });

  const visible = defineModel<boolean>('visible', { required: true });
  const props = defineProps<{ recordId: string }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const TRANSIENT_STATUSES: CallReviewStatus[] = ['PENDING_TRANSCRIBE', 'TRANSCRIBING', 'ANALYZING'];

  const detail = ref<CallReviewDetailResponse | null>(null);
  const loading = ref(false);
  const retrying = ref(false);
  let pollTimer: ReturnType<typeof setInterval> | undefined;

  const transient = computed(() => Boolean(detail.value && TRANSIENT_STATUSES.includes(detail.value.status)));

  const statusMeta = computed(() => {
    const status = detail.value?.status ?? 'DONE';
    const map: Record<CallReviewStatus, { type: 'info' | 'success' | 'warning' | 'error' }> = {
      PENDING_TRANSCRIBE: { type: 'warning' },
      TRANSCRIBING: { type: 'warning' },
      ANALYZING: { type: 'warning' },
      DONE: { type: 'success' },
      FAILED: { type: 'error' },
    };
    return {
      type: map[status]?.type ?? 'info',
      label: t(`workbench.callReview.status.${status}`),
    };
  });

  function formatTime(value?: number | string): string {
    if (!value) {
      return '-';
    }
    if (typeof value === 'string') {
      return value;
    }
    return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
  }

  function formatDuration(value?: number): string {
    return value === null || value === undefined ? '-' : `${value}s`;
  }

  async function loadDetail() {
    if (!props.recordId) {
      return;
    }
    detail.value = await getCallReviewDetail(props.recordId);
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = undefined;
    }
  }

  function startPolling() {
    stopPolling();
    pollTimer = setInterval(async () => {
      try {
        await loadDetail();
        if (detail.value && !transient.value) {
          stopPolling();
          emit('refresh');
        }
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      }
    }, 3000);
  }

  async function open() {
    detail.value = null;
    loading.value = true;
    try {
      await loadDetail();
      if (transient.value) {
        startPolling();
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  watch(
    () => visible.value,
    (isVisible) => {
      if (isVisible) {
        open();
      } else {
        stopPolling();
      }
    }
  );

  const followVisible = ref(false);
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

  function handleToFollow() {
    if (!detail.value?.review) {
      return;
    }
    followInitialValues.value = { content: buildFollowContent(detail.value.review) };
    followVisible.value = true;
  }

  function handleFollowSaved() {
    Message.success(t('common.operationSuccess'));
    followVisible.value = false;
  }

  async function handleRetry() {
    if (!props.recordId) {
      return;
    }
    retrying.value = true;
    try {
      await retryCallReview(props.recordId);
      Message.success(t('workbench.callReview.retrySuccess'));
      await loadDetail();
      if (transient.value) {
        startPolling();
      }
      emit('refresh');
    } finally {
      retrying.value = false;
    }
  }

  onBeforeUnmount(() => {
    stopPolling();
  });
</script>

<style scoped lang="less"></style>
