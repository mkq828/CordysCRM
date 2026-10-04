<template>
  <CrmModal
    v-model:show="visible"
    preset="card"
    :title="t('workbench.callReview.config.title')"
    :ok-loading="saving"
    :footer="true"
    :positive-text="t('common.confirm')"
    :negative-text="t('common.cancel')"
    @confirm="handleSave"
  >
    <n-spin :show="loading" class="call-review-config-modal">
      <div class="flex flex-col gap-[16px] pt-[8px]">
        <n-form-item :label="t('workbench.callReview.config.callbackUrl')" label-placement="top" :show-feedback="false">
          <n-input-group>
            <n-input :value="config.callbackUrl" readonly />
            <n-button class="w-[64px] shrink-0" @click="copy(config.callbackUrl)">
              {{ t('workbench.callReview.config.copy') }}
            </n-button>
          </n-input-group>
        </n-form-item>

        <div class="grid grid-cols-2 gap-x-[16px]">
          <n-form-item :label="t('workbench.callReview.config.appKey')" label-placement="top" :show-feedback="false">
            <n-input-group>
              <n-input :value="config.appKey" readonly />
              <n-button class="w-[64px] shrink-0" @click="copy(config.appKey)">
                {{ t('workbench.callReview.config.copy') }}
              </n-button>
            </n-input-group>
          </n-form-item>
          <n-form-item :label="t('workbench.callReview.config.secretKey')" label-placement="top" :show-feedback="false">
            <n-input-group>
              <n-input :value="config.secretKey" readonly />
              <n-button class="w-[64px] shrink-0" @click="copy(config.secretKey)">
                {{ t('workbench.callReview.config.copy') }}
              </n-button>
            </n-input-group>
          </n-form-item>
        </div>

        <div class="flex items-center justify-between">
          <div class="flex items-center gap-[8px] text-[13px] text-[var(--text-n1)]">
            {{ t('workbench.callReview.config.enable') }}
            <n-switch v-model:value="config.enable" />
          </div>
          <n-button size="small" type="error" ghost @click="handleResetKey">
            {{ t('workbench.callReview.config.resetKey') }}
          </n-button>
        </div>

        <div>
          <div class="mb-[4px] text-[13px] font-semibold text-[var(--text-n1)]">
            {{ t('workbench.callReview.config.fieldMapping') }}
          </div>
          <div class="mb-[12px] text-[12px] text-orange-500">
            {{ t('workbench.callReview.config.fieldMappingTip') }}
          </div>
          <div class="mb-[8px] flex items-center gap-[12px] text-[12px] text-[var(--text-n3)]">
            <div class="w-[120px] shrink-0">{{ t('workbench.callReview.config.standardField') }}</div>
            <div class="flex-1">{{ t('workbench.callReview.config.supplierField') }}</div>
          </div>
          <div class="flex flex-col gap-[12px]">
            <div v-for="field in standardFields" :key="field.key" class="flex items-center gap-[12px]">
              <div class="w-[120px] shrink-0 text-[13px] text-[var(--text-n2)]">
                {{ t(field.labelKey) }}
                <span v-if="field.required" class="text-[var(--error)]">*</span>
              </div>
              <n-input
                v-model:value="fieldMapping[field.key]"
                :placeholder="t('workbench.callReview.config.supplierFieldPlaceholder')"
              />
            </div>
          </div>
        </div>
      </div>
    </n-spin>
  </CrmModal>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue';
  import { NButton, NFormItem, NInput, NInputGroup, NSpin, NSwitch, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CallReviewConfigResponse } from '@lib/shared/models/callReview';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { getCallReviewConfig, resetCallReviewConfigKey, saveCallReviewConfig } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const visible = defineModel<boolean>('visible', { required: true });

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  interface StandardField {
    key: string;
    labelKey: string;
    required?: boolean;
  }

  const standardFields: StandardField[] = [
    { key: 'caller', labelKey: 'workbench.callReview.caller' },
    { key: 'callee', labelKey: 'workbench.callReview.callee' },
    { key: 'customerPhone', labelKey: 'workbench.callReview.customerPhone' },
    { key: 'callTime', labelKey: 'workbench.callReview.callTime' },
    { key: 'duration', labelKey: 'workbench.callReview.duration' },
    { key: 'recordUrl', labelKey: 'workbench.callReview.recordUrl', required: true },
  ];

  const loading = ref(false);
  const saving = ref(false);
  const config = reactive<CallReviewConfigResponse>({
    appKey: '',
    secretKey: '',
    callbackUrl: '',
    fieldMapping: {},
    enable: false,
  });
  const fieldMapping = reactive<Record<string, string>>({});

  function applyConfig(data: CallReviewConfigResponse) {
    config.appKey = data.appKey || '';
    config.secretKey = data.secretKey || '';
    config.callbackUrl = data.callbackUrl || '';
    config.enable = Boolean(data.enable);
    standardFields.forEach((field) => {
      fieldMapping[field.key] = data.fieldMapping?.[field.key] || '';
    });
  }

  async function loadConfig() {
    loading.value = true;
    try {
      const data = await getCallReviewConfig();
      applyConfig(data);
    } finally {
      loading.value = false;
    }
  }

  async function copy(text?: string) {
    if (!text) {
      return;
    }
    try {
      await navigator.clipboard.writeText(text);
      Message.success(t('workbench.callReview.config.copied'));
    } catch {
      Message.error(t('common.operationFailed'));
    }
  }

  function buildMapping(): Record<string, string> {
    const result: Record<string, string> = {};
    standardFields.forEach((field) => {
      const value = fieldMapping[field.key]?.trim();
      if (value) {
        result[field.key] = value;
      }
    });
    return result;
  }

  async function handleSave() {
    const mapping = buildMapping();
    if (!mapping.recordUrl) {
      Message.warning(t('workbench.callReview.config.fieldMappingTip'));
      return;
    }
    saving.value = true;
    try {
      const data = await saveCallReviewConfig({ fieldMapping: mapping, enable: config.enable });
      applyConfig(data);
      Message.success(t('workbench.callReview.saveSuccess'));
      visible.value = false;
    } finally {
      saving.value = false;
    }
  }

  function handleResetKey() {
    openModal({
      type: 'warning',
      title: t('workbench.callReview.config.resetKey'),
      content: t('workbench.callReview.config.resetKeyConfirm'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        const data = await resetCallReviewConfigKey();
        applyConfig(data);
        Message.success(t('workbench.callReview.saveSuccess'));
      },
    });
  }

  onMounted(() => {
    loadConfig();
  });
</script>

<style scoped lang="less">
  .call-review-config-modal {
    min-height: 200px;
  }
</style>
