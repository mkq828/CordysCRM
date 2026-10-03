<template>
  <CrmModal
    v-model:show="visible"
    preset="card"
    :title="t('workbench.callReview.upload')"
    :ok-loading="submitting"
    :footer="true"
    :positive-text="t('common.confirm')"
    :negative-text="t('common.cancel')"
    @confirm="handleSubmit"
  >
    <n-form ref="formRef" :model="form" label-placement="top" class="call-review-upload-form">
      <n-form-item :label="t('workbench.callReview.recordUrl')">
        <n-input v-model:value="form.recordUrl" :placeholder="t('workbench.callReview.recordUrlPlaceholder')" />
      </n-form-item>

      <n-form-item :label="t('workbench.callReview.uploadFileTip')">
        <div class="flex items-center gap-[8px]">
          <n-upload :custom-request="handleUpload" :show-file-list="false" accept="audio/*" :disabled="submitting">
            <n-button size="small" secondary :disabled="submitting">
              <template #icon>
                <CrmIcon type="iconicon_cloud_upload" :size="16" />
              </template>
              {{ t('workbench.callReview.uploadFileTip') }}
            </n-button>
          </n-upload>
          <n-tag v-if="uploadedName" size="small" closable @close="clearFile">
            {{ uploadedName }}
          </n-tag>
        </div>
      </n-form-item>

      <n-form-item :label="t('workbench.callReview.customer')">
        <n-select
          v-model:value="form.customerId"
          clearable
          filterable
          remote
          :loading="customerLoading"
          :placeholder="t('workbench.callReview.customer')"
          :options="customerOptions"
          @search="handleSearchCustomer"
        />
      </n-form-item>

      <div class="grid grid-cols-2 gap-x-[16px]">
        <n-form-item :label="t('workbench.callReview.caller')">
          <n-input v-model:value="form.caller" />
        </n-form-item>
        <n-form-item :label="t('workbench.callReview.callee')">
          <n-input v-model:value="form.callee" />
        </n-form-item>
        <n-form-item :label="t('workbench.callReview.customerPhone')">
          <n-input v-model:value="form.customerPhone" />
        </n-form-item>
        <n-form-item :label="t('workbench.callReview.callTime')">
          <CrmDatePicker v-model:value="form.callTime" type="datetime" clearable class="w-full" />
        </n-form-item>
        <n-form-item :label="t('workbench.callReview.duration')">
          <n-input-number v-model:value="form.duration" :min="0" :precision="0" class="w-full" />
        </n-form-item>
      </div>

      <div class="text-[12px] text-orange-500">{{ t('workbench.callReview.recordUrlTip') }}</div>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue';
  import {
    NButton,
    NForm,
    NFormItem,
    NInput,
    NInputNumber,
    NSelect,
    NTag,
    NUpload,
    type UploadCustomRequestOptions,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDatePicker from '@/components/pure/crm-date-picker/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { getCustomerOptions, uploadCallReview, uploadTempAttachment } from '@/api/modules';

  const visible = defineModel<boolean>('visible', { required: true });
  const emit = defineEmits<{
    (e: 'saved'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const submitting = ref(false);
  const uploadedName = ref('');

  interface UploadForm {
    recordUrl: string;
    recordAttachmentId?: string;
    customerId: string | null;
    caller: string;
    callee: string;
    customerPhone: string;
    callTime: number | null;
    duration: number | null;
  }

  const form = reactive<UploadForm>({
    recordUrl: '',
    recordAttachmentId: undefined,
    customerId: null,
    caller: '',
    callee: '',
    customerPhone: '',
    callTime: null,
    duration: null,
  });

  function resetForm() {
    form.recordUrl = '';
    form.recordAttachmentId = undefined;
    form.customerId = null;
    form.caller = '';
    form.callee = '';
    form.customerPhone = '';
    form.callTime = null;
    form.duration = null;
    uploadedName.value = '';
  }

  const customerLoading = ref(false);
  const customerOptions = ref<{ label: string; value: string }[]>([]);

  async function loadCustomerOptions(keyword?: string) {
    customerLoading.value = true;
    try {
      const res = await getCustomerOptions({ current: 1, pageSize: 10, keyword });
      customerOptions.value = (res.list || []).map((item) => ({
        label: item.name,
        value: String(item.id),
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      customerLoading.value = false;
    }
  }

  function handleSearchCustomer(keyword: string) {
    loadCustomerOptions(keyword);
  }

  async function handleUpload({ file, onFinish, onError }: UploadCustomRequestOptions) {
    try {
      const res = await uploadTempAttachment(file.file as File);
      const id = res.data?.[0];
      if (!id) {
        throw new Error('upload empty');
      }
      form.recordAttachmentId = id;
      uploadedName.value = file.name;
      onFinish();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      Message.error(t('common.operationFailed'));
      onError();
    }
  }

  function clearFile() {
    form.recordAttachmentId = undefined;
    uploadedName.value = '';
  }

  async function handleSubmit() {
    if (!form.recordUrl?.trim() && !form.recordAttachmentId) {
      Message.warning(t('workbench.callReview.requireRecordTip'));
      return;
    }
    submitting.value = true;
    try {
      await uploadCallReview({
        recordUrl: form.recordUrl?.trim() || undefined,
        recordAttachmentId: form.recordAttachmentId,
        customerId: form.customerId || undefined,
        caller: form.caller?.trim() || undefined,
        callee: form.callee?.trim() || undefined,
        customerPhone: form.customerPhone?.trim() || undefined,
        callTime: form.callTime ?? undefined,
        duration: form.duration ?? undefined,
      });
      resetForm();
      visible.value = false;
      emit('saved');
    } finally {
      submitting.value = false;
    }
  }

  onMounted(() => {
    loadCustomerOptions();
  });
</script>

<style scoped lang="less">
  .call-review-upload-form {
    padding-top: 8px;
  }
</style>
