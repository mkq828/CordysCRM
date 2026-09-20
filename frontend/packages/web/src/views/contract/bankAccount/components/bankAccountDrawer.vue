<template>
  <CrmDrawer
    v-model:show="visible"
    width="600"
    :show-continue="!form.id"
    :title="form.id ? t('contract.bankAccount.update') : t('contract.bankAccount.add')"
    :ok-text="form.id ? t('common.update') : t('common.add')"
    :loading="loading"
    @confirm="handleConfirm(false)"
    @continue="handleConfirm(true)"
    @cancel="cancelHandler"
  >
    <n-scrollbar>
      <n-form ref="formRef" :model="form">
        <n-form-item
          path="name"
          :label="t('contract.bankAccount.name')"
          :rule="[
            {
              required: true,
              message: t('common.notNull', { value: t('contract.bankAccount.name') }),
              trigger: ['input', 'blur'],
            },
          ]"
        >
          <n-input v-model:value="form.name" allow-clear :maxlength="255" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item
          path="type"
          :label="t('contract.bankAccount.type')"
          :rule="[
            {
              required: true,
              message: t('common.notNull', { value: t('contract.bankAccount.type') }),
              trigger: ['change', 'blur'],
            },
          ]"
        >
          <n-select v-model:value="form.type" :options="typeOptions" :placeholder="t('common.pleaseSelect')" />
        </n-form-item>
        <n-form-item
          v-if="form.type === BankAccountTypeEnum.BANK_CARD"
          path="openingBank"
          :label="t('contract.bankAccount.openingBank')"
        >
          <n-input
            v-model:value="form.openingBank"
            allow-clear
            :maxlength="255"
            :placeholder="t('common.pleaseInput')"
          />
        </n-form-item>
        <n-form-item path="bankAccount" :label="t('contract.bankAccount.bankAccount')">
          <n-input
            v-model:value="form.bankAccount"
            allow-clear
            :maxlength="255"
            :placeholder="t('common.pleaseInput')"
          />
        </n-form-item>
        <n-form-item path="accountHolder" :label="t('contract.bankAccount.accountHolder')">
          <n-input
            v-model:value="form.accountHolder"
            allow-clear
            :maxlength="255"
            :placeholder="t('common.pleaseInput')"
          />
        </n-form-item>
        <n-form-item v-if="form.type !== BankAccountTypeEnum.BANK_CARD" :label="t('contract.bankAccount.qrcode')">
          <n-upload
            v-model:file-list="qrcodeFileList"
            list-type="image-card"
            :max="1"
            accept="image/*"
            :custom-request="qrcodeCustomRequest"
            @remove="handleQrcodeRemove"
          />
        </n-form-item>
        <n-form-item path="remark" :label="t('common.remark')">
          <n-input
            v-model:value="form.remark"
            allow-clear
            type="textarea"
            :rows="3"
            :maxlength="500"
            :placeholder="t('common.pleaseInput')"
          />
        </n-form-item>
      </n-form>
    </n-scrollbar>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import {
    FormInst,
    NForm,
    NFormItem,
    NInput,
    NScrollbar,
    NSelect,
    NUpload,
    type UploadCustomRequestOptions,
    type UploadFileInfo,
    useMessage,
  } from 'naive-ui';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { BankAccountTypeEnum } from '@lib/shared/enums/bankAccountEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { SaveBankAccountParams } from '@lib/shared/models/contract';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import { addBankAccount, getBankAccountDetail, updateBankAccount, uploadTempFile } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  import initBankAccountForm from '../config';

  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  const props = defineProps<{
    sourceId: string;
  }>();

  const emit = defineEmits<{
    (e: 'load'): void;
    (e: 'cancel'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const form = ref<SaveBankAccountParams>({
    ...initBankAccountForm,
  });

  const formRef = ref<FormInst | null>(null);

  const typeOptions = computed(() => [
    { label: t('contract.bankAccount.typeBankCard'), value: BankAccountTypeEnum.BANK_CARD },
    { label: t('contract.bankAccount.typeWechat'), value: BankAccountTypeEnum.WECHAT },
    { label: t('contract.bankAccount.typeAlipay'), value: BankAccountTypeEnum.ALIPAY },
  ]);

  const qrcodeFileList = ref<UploadFileInfo[]>([]);

  function setQrcodeFileList(qrcode: string) {
    if (qrcode) {
      qrcodeFileList.value = [
        {
          id: qrcode,
          name: 'qrcode',
          thumbnailUrl: `${PreviewPictureUrl}/${qrcode}?userId=${userStore.userInfo.id}`,
          url: `${PreviewPictureUrl}/${qrcode}?userId=${userStore.userInfo.id}`,
          status: 'finished',
          type: 'image/*',
        },
      ];
    } else {
      qrcodeFileList.value = [];
    }
  }

  async function qrcodeCustomRequest({ file, onFinish, onError }: UploadCustomRequestOptions) {
    try {
      const res = await uploadTempFile(file.file);
      [form.value.qrcode] = res.data;
      onFinish();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      onError();
    }
  }

  function handleQrcodeRemove() {
    form.value.qrcode = '';
  }

  function cancelHandler() {
    form.value = { ...initBankAccountForm };
    setQrcodeFileList('');
    emit('cancel');
    visible.value = false;
  }

  const loading = ref<boolean>(false);
  async function handleSave(isContinue: boolean) {
    try {
      loading.value = true;
      if (form.value.id) {
        await updateBankAccount(form.value);
        Message.success(t('common.updateSuccess'));
      } else {
        await addBankAccount(form.value);
        Message.success(t('common.addSuccess'));
      }
      if (isContinue) {
        form.value = { ...initBankAccountForm };
        setQrcodeFileList('');
      } else {
        cancelHandler();
      }
      emit('load');
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      loading.value = false;
    }
  }

  function handleConfirm(isContinue: boolean) {
    formRef.value?.validate(async (error) => {
      if (!error) {
        handleSave(isContinue);
      }
    });
  }

  async function initDetail() {
    if (!props.sourceId) return;
    try {
      const result = await getBankAccountDetail(props.sourceId);
      form.value = { ...result };
      setQrcodeFileList(result.qrcode);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => form.value.type,
    (type) => {
      if (type === BankAccountTypeEnum.BANK_CARD) {
        form.value.qrcode = '';
        setQrcodeFileList('');
      } else {
        form.value.openingBank = '';
      }
    }
  );

  watch(
    () => visible.value,
    (newVal) => {
      if (newVal) {
        form.value = { ...initBankAccountForm };
        setQrcodeFileList('');
        initDetail();
      }
    }
  );
</script>

<style scoped></style>
