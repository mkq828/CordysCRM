<template>
  <div
    class="feedback-bug-floating-entry large-box-shadow n-btn-outline-primary fixed z-[999] flex h-[48px] w-[48px] cursor-pointer select-none items-center justify-center rounded-full bg-[var(--text-n10)] text-[12px] font-medium text-[var(--text-n1)]"
    :style="floatingStyle"
    @pointerdown="handlePointerDown"
    @click="handleFloatingClick"
  >
    {{ t('feedback.button') }}
  </div>

  <CrmDrawer v-model:show="showDrawer" :title="t('feedback.title')" :width="560" :footer="false">
    <div class="flex flex-col gap-4 p-4">
      <div>
        <div class="mb-1 text-sm">{{ t('feedback.description') }}</div>
        <n-input
          v-model:value="description"
          type="textarea"
          :placeholder="t('feedback.descriptionPlaceholder')"
          :rows="4"
        />
      </div>
      <div>
        <div class="mb-1 text-sm">{{ t('feedback.steps') }}</div>
        <n-input v-model:value="steps" type="textarea" :placeholder="t('feedback.stepsPlaceholder')" :rows="3" />
      </div>
      <div class="flex items-center justify-between">
        <span class="text-sm">{{ t('feedback.includeScreenshot') }}</span>
        <n-switch v-model:value="includeScreenshot" />
      </div>
      <div class="text-xs text-orange-500">
        {{ t('feedback.collectedInfo') }}：{{ collected.errors }} {{ t('feedback.errors') }} /
        {{ collected.failedRequests }}
        {{ t('feedback.failedRequests') }}
      </div>
      <div class="flex justify-end gap-2">
        <n-button @click="showDrawer = false">{{ t('feedback.cancel') }}</n-button>
        <n-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ t('feedback.submit') }}
        </n-button>
      </div>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
  import { useRoute } from 'vue-router';
  import { NButton, NInput, NSwitch } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import { submitBugReport } from '@/api/modules/system/bugReport';
  import useDiscreteApi from '@/hooks/useDiscreteApi';
  import useAppStore from '@/store/modules/app';
  import useUserStore from '@/store/modules/user';
  import { type BugEnv, getCollectedCount, getCollectedData } from '@/utils/bugCollector';

  import html2canvas from 'html2canvas-pro';

  const { t } = useI18n();
  const { message } = useDiscreteApi();
  const userStore = useUserStore();
  const appStore = useAppStore();
  const route = useRoute();

  const showDrawer = ref(false);
  const description = ref('');
  const steps = ref('');
  const includeScreenshot = ref(false);
  const submitting = ref(false);

  const collected = computed(() => getCollectedCount());

  // 悬浮按钮拖拽（参考 AiChatFloatingEntry）
  interface FloatingPosition {
    right: number;
    bottom: number;
  }

  const FLOATING_SIZE = 48;
  const DEFAULT_GAP = 24;
  const DRAG_THRESHOLD = 4;
  const STORAGE_KEY = 'crm_feedback_bug_floating_entry_position';

  const position = ref<FloatingPosition>({
    right: DEFAULT_GAP,
    bottom: DEFAULT_GAP + FLOATING_SIZE + 16,
  });
  const isDragging = ref(false);
  const ignoreNextClick = ref(false);

  const floatingStyle = computed(() => ({
    right: `${position.value.right}px`,
    bottom: `${position.value.bottom}px`,
  }));

  function clampOffset(offset: number, viewportSize: number): number {
    const maxOffset = Math.max(DEFAULT_GAP, viewportSize - FLOATING_SIZE - DEFAULT_GAP);
    return Math.min(Math.max(DEFAULT_GAP, offset), maxOffset);
  }

  function normalizePosition(next = position.value): FloatingPosition {
    return {
      right: clampOffset(next.right, window.innerWidth),
      bottom: clampOffset(next.bottom, window.innerHeight),
    };
  }

  function initPosition(): void {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') as FloatingPosition | null;
      position.value = normalizePosition(saved ?? position.value);
    } catch {
      position.value = normalizePosition();
    }
  }

  function savePosition(): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(position.value));
  }

  function handleFloatingClick(): void {
    if (ignoreNextClick.value) {
      ignoreNextClick.value = false;
      return;
    }
    showDrawer.value = true;
  }

  function handlePointerDown(event: PointerEvent): void {
    if (event.button !== 0) {
      return;
    }

    const startPosition = { ...position.value };
    const startPointer = { x: event.clientX, y: event.clientY };

    const handlePointerMove = (moveEvent: PointerEvent) => {
      const deltaX = moveEvent.clientX - startPointer.x;
      const deltaY = moveEvent.clientY - startPointer.y;

      if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
        isDragging.value = true;
      }

      if (isDragging.value) {
        position.value = normalizePosition({
          right: startPosition.right - deltaX,
          bottom: startPosition.bottom - deltaY,
        });
      }
    };

    const handlePointerUp = () => {
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', handlePointerUp);

      if (isDragging.value) {
        ignoreNextClick.value = true;
        savePosition();
      }

      isDragging.value = false;
    };

    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', handlePointerUp);
  }

  function buildEnv(): BugEnv {
    const roles = (userStore.userInfo.roles ?? [])
      .map((role: any) => role?.name ?? role?.dataScope ?? '')
      .filter(Boolean)
      .join('、');
    return {
      orgId: appStore.orgId || '',
      userId: userStore.userInfo.id || '',
      userName: userStore.userInfo.name || '',
      roles: roles || '-',
      route: route.fullPath || '',
      version: appStore.versionInfo.currentVersion || '',
      userAgent: navigator.userAgent,
      screen: `${window.innerWidth}x${window.innerHeight}`,
      language: navigator.language || localStorage.getItem('CRM-locale') || '',
    };
  }

  async function captureScreenshot(): Promise<string | undefined> {
    if (!includeScreenshot.value) {
      return undefined;
    }
    try {
      const canvas = await html2canvas(document.body, { scale: 0.5, useCORS: true });
      return canvas.toDataURL('image/png');
    } catch {
      return undefined;
    }
  }

  async function handleSubmit(): Promise<void> {
    if (!description.value.trim()) {
      message.warning(t('feedback.descriptionRequired'));
      return;
    }
    submitting.value = true;
    try {
      const screenshot = await captureScreenshot();
      const env = buildEnv();
      const data = getCollectedData();
      const lastTraceId = data.failedRequests.find((r) => r.traceId)?.traceId;
      await submitBugReport({
        description: description.value.trim(),
        steps: steps.value.trim() || undefined,
        screenshot,
        route: env.route,
        version: env.version,
        roles: env.roles,
        userAgent: env.userAgent,
        screen: env.screen,
        language: env.language,
        recentErrors: JSON.stringify(data.errors),
        failedRequests: JSON.stringify(data.failedRequests),
        traceId: lastTraceId,
      });
      message.success(t('feedback.submitSuccess'));
      showDrawer.value = false;
      description.value = '';
      steps.value = '';
      includeScreenshot.value = false;
    } catch {
      message.error(t('feedback.submitFailed'));
    } finally {
      submitting.value = false;
    }
  }

  function handleResize(): void {
    position.value = normalizePosition();
    savePosition();
  }

  onMounted(() => {
    initPosition();
    window.addEventListener('resize', handleResize);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize);
  });
</script>
