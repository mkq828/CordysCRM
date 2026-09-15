<template>
  <div class="register-page">
    <div class="register-card">
      <div v-if="!submitted" class="register-form-wrapper">
        <div class="register-title">{{ t('register.title') }}</div>
        <n-tabs v-model:value="activeTab" type="line" justify-content="space-evenly" class="register-tabs">
          <n-tab-pane name="PERSONAL" :tab="t('register.personal')" />
          <n-tab-pane name="ENTERPRISE" :tab="t('register.enterprise')" />
        </n-tabs>
        <n-form ref="formRef" :model="formModel" :rules="rules">
          <n-form-item v-if="activeTab === 'ENTERPRISE'" path="name" :label="t('register.companyName')">
            <n-input v-model:value="formModel.name" :placeholder="t('register.companyName.placeholder')" />
          </n-form-item>
          <n-form-item v-if="activeTab === 'PERSONAL'" path="name" :label="t('register.name')">
            <n-input v-model:value="formModel.name" :placeholder="t('register.name.placeholder')" />
          </n-form-item>
          <template v-if="activeTab === 'ENTERPRISE'">
            <n-form-item path="unifiedSocialCreditCode" :label="t('register.unifiedSocialCreditCode')">
              <n-input
                v-model:value="formModel.unifiedSocialCreditCode"
                :placeholder="t('register.unifiedSocialCreditCode.placeholder')"
              />
            </n-form-item>
            <n-form-item path="legalPersonName" :label="t('register.legalPersonName')">
              <n-input
                v-model:value="formModel.legalPersonName"
                :placeholder="t('register.legalPersonName.placeholder')"
              />
            </n-form-item>
            <n-form-item path="idCard" :label="t('register.legalPersonIdCard')">
              <n-input v-model:value="formModel.idCard" :placeholder="t('register.idCard.placeholder')" />
            </n-form-item>
          </template>
          <n-form-item v-if="activeTab === 'PERSONAL'" path="idCard" :label="t('register.idCard')">
            <n-input v-model:value="formModel.idCard" :placeholder="t('register.idCard.placeholder')" />
          </n-form-item>
          <n-form-item path="phone" :label="t('register.phone')">
            <n-input v-model:value="formModel.phone" :maxlength="11" :placeholder="t('register.phone.placeholder')" />
          </n-form-item>
          <n-form-item path="password" :label="t('register.password')">
            <n-input
              v-model:value="formModel.password"
              type="password"
              show-password-on="click"
              :placeholder="t('register.password.placeholder')"
            />
          </n-form-item>
          <n-form-item path="confirmPassword" :label="t('register.confirmPassword')">
            <n-input
              v-model:value="formModel.confirmPassword"
              type="password"
              show-password-on="click"
              :placeholder="t('register.confirmPassword.placeholder')"
            />
          </n-form-item>
          <n-form-item
            v-if="activeTab === 'ENTERPRISE'"
            path="businessLicenseAttachmentId"
            :label="t('register.businessLicense')"
          >
            <div class="flex w-full items-center gap-[12px]">
              <n-upload :show-file-list="false" accept="image/*" :max="1" :custom-request="uploadLicense">
                <n-button>{{ licenseFileName || t('register.uploadLicense') }}</n-button>
              </n-upload>
              <span v-if="licenseFileName" class="one-line-text text-[12px] text-[var(--text-n4)]">{{
                licenseFileName
              }}</span>
            </div>
          </n-form-item>
        </n-form>
        <n-button type="primary" block size="large" :loading="loading" @click="handleSubmit">
          {{ t('register.submit') }}
        </n-button>
        <div class="mt-[16px] flex justify-center">
          <n-button text type="primary" @click="goLogin">{{ t('register.backToLogin') }}</n-button>
        </div>
      </div>

      <div v-else class="register-result-wrapper">
        <n-result status="success" :title="t('register.submit.success')">
          <template #footer>
            <div class="flex flex-col items-center gap-[16px]">
              <n-button type="primary" @click="goLogin">{{ t('register.backToLogin') }}</n-button>
              <div class="register-status-query">
                <div class="mb-[8px] text-[var(--text-n2)]">{{ t('register.status.query') }}</div>
                <div class="flex items-center gap-[8px]">
                  <n-input
                    v-model:value="statusPhone"
                    :placeholder="t('register.phone.placeholder')"
                    class="w-[220px]"
                  />
                  <n-button :loading="statusLoading" @click="queryStatus">{{ t('register.status.queryBtn') }}</n-button>
                </div>
                <div v-if="statusResult" class="mt-[12px] text-left">
                  <n-tag :type="statusTagType">{{ statusText }}</n-tag>
                  <div v-if="statusResult.verifyRemark" class="mt-[8px] text-[var(--text-n3)]">
                    {{ t('register.status.remark') }}：{{ statusResult.verifyRemark }}
                  </div>
                </div>
              </div>
            </div>
          </template>
        </n-result>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import {
    type FormInst,
    type FormRules,
    NButton,
    NForm,
    NFormItem,
    NInput,
    NResult,
    NTabPane,
    NTabs,
    NTag,
    NUpload,
    type UploadCustomRequestOptions,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { encrypted } from '@lib/shared/method';
  import type { RegisterStatusResult } from '@lib/shared/models/system/register';

  import { registerApply, registerStatus, uploadTempAttachment } from '@/api/modules';
  import useAppStore from '@/store/modules/app';

  const router = useRouter();
  const { t } = useI18n();
  const Message = useMessage();
  const appStore = useAppStore();

  const formRef = ref<FormInst | null>(null);
  const activeTab = ref<'PERSONAL' | 'ENTERPRISE'>('PERSONAL');
  const loading = ref(false);
  const submitted = ref(false);

  const licenseFileName = ref('');
  const statusPhone = ref('');
  const statusLoading = ref(false);
  const statusResult = ref<RegisterStatusResult | null>(null);

  const formModel = ref({
    name: '',
    phone: '',
    password: '',
    confirmPassword: '',
    idCard: '',
    unifiedSocialCreditCode: '',
    legalPersonName: '',
    businessLicenseAttachmentId: '',
  });

  const rules = computed<FormRules>(() => ({
    name: {
      required: true,
      message: activeTab.value === 'PERSONAL' ? t('register.name.errMsg') : t('register.companyName.errMsg'),
      trigger: ['input', 'blur'],
    },
    phone: {
      required: true,
      validator: (_rule: unknown, value: string) => {
        if (!/^1\d{10}$/.test(value || '')) {
          return new Error(t('register.phone.errMsg'));
        }
        return true;
      },
      trigger: ['input', 'blur'],
    },
    password: {
      required: true,
      message: t('register.password.errMsg'),
      trigger: ['input', 'blur'],
    },
    confirmPassword: {
      required: true,
      validator: (_rule: unknown, value: string) => {
        if (value !== formModel.value.password) {
          return new Error(t('register.confirmPassword.errMsg'));
        }
        return true;
      },
      trigger: ['input', 'blur'],
    },
    idCard: {
      required: true,
      message: t('register.idCard.errMsg'),
      trigger: ['input', 'blur'],
    },
    unifiedSocialCreditCode: {
      required: true,
      message: t('register.unifiedSocialCreditCode.errMsg'),
      trigger: ['input', 'blur'],
    },
    legalPersonName: {
      required: true,
      message: t('register.legalPersonName.errMsg'),
      trigger: ['input', 'blur'],
    },
    businessLicenseAttachmentId: {
      required: true,
      message: t('register.businessLicense.errMsg'),
      trigger: ['change'],
    },
  }));

  async function uploadLicense({ file, onFinish, onError }: UploadCustomRequestOptions) {
    try {
      const res = await uploadTempAttachment(file.file as File);
      const [attachmentId] = res.data;
      formModel.value.businessLicenseAttachmentId = attachmentId;
      licenseFileName.value = (file.file as File).name;
      onFinish();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      onError();
    }
  }

  async function handleSubmit() {
    if (loading.value) return;
    formRef.value?.validate(async (errors) => {
      if (errors) return;
      loading.value = true;
      try {
        // 确保公钥就绪后再加密密码
        await appStore.initPublicKey();
        await registerApply({
          type: activeTab.value,
          name: formModel.value.name,
          phone: formModel.value.phone,
          password: encrypted(formModel.value.password) || '',
          idCard: formModel.value.idCard,
          unifiedSocialCreditCode: formModel.value.unifiedSocialCreditCode || undefined,
          legalPersonName: formModel.value.legalPersonName || undefined,
          businessLicenseAttachmentId: formModel.value.businessLicenseAttachmentId || undefined,
        });
        Message.success(t('register.submit.success'));
        statusPhone.value = formModel.value.phone;
        submitted.value = true;
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      } finally {
        loading.value = false;
      }
    });
  }

  async function queryStatus() {
    if (!statusPhone.value) {
      Message.warning(t('register.phone.errMsg'));
      return;
    }
    statusLoading.value = true;
    try {
      statusResult.value = await registerStatus({ phone: statusPhone.value });
      if (!statusResult.value) {
        Message.info(t('register.status.notFound'));
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      statusLoading.value = false;
    }
  }

  const statusText = computed(() => {
    const s = statusResult.value?.verifyStatus;
    if (s === 'APPROVED') return t('register.status.approved');
    if (s === 'REJECTED') return t('register.status.rejected');
    if (s === 'PENDING') return t('register.status.pending');
    return '';
  });

  const statusTagType = computed(() => {
    const s = statusResult.value?.verifyStatus;
    if (s === 'APPROVED') return 'success';
    if (s === 'REJECTED') return 'error';
    return 'warning';
  });

  function goLogin() {
    router.push({ name: 'login' });
  }
</script>

<style lang="less" scoped>
  .register-page {
    @apply flex min-h-screen items-center justify-center;

    padding: 24px;
    background-color: var(--body-color, #f5f6fa);
  }
  .register-card {
    padding: 40px;
    width: 480px;
    border-radius: var(--border-radius-large);
    background-color: var(--text-n10);
    box-shadow: 0 8px 10px 0 #3232330d, 0 16px 24px 0 #3232330d, 0 6px 30px 0 #3232330d;
  }
  .register-title {
    @apply mb-[24px] text-center;

    font-size: 22px;
    font-weight: 600;
    color: var(--primary-8);
  }
  .register-tabs {
    margin-bottom: 16px;
  }
  .register-status-query {
    width: 100%;
  }
</style>
