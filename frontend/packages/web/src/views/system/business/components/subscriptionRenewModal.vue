<template>
  <CrmModal
    v-model:show="showModal"
    size="large"
    preset="card"
    :title="t('system.personal.renew.title')"
    @cancel="cancel"
  >
    <n-scrollbar style="max-height: 62vh">
      <div class="flex flex-col gap-[16px]">
        <!-- 当前版本 -->
        <div class="flex items-center gap-[12px]">
          <span class="w-[90px] shrink-0 text-right text-[var(--text-n4)]">
            {{ t('system.personal.renew.currentVersion') }}
          </span>
          <span class="text-[var(--text-n1)]">{{ currentLabel }}</span>
        </div>

        <!-- 目标版本 -->
        <div class="flex items-start gap-[12px]">
          <span class="w-[90px] shrink-0 pt-[6px] text-right text-[var(--text-n4)]">
            {{ t('system.personal.renew.targetVersion') }}
          </span>
          <div class="flex flex-1 flex-col gap-[8px]">
            <div
              v-for="edition in editions"
              :key="edition.code"
              class="flex cursor-pointer items-center gap-[12px] rounded border px-[12px] py-[10px] transition-colors"
              :class="
                targetEdition === edition.code
                  ? 'border-[var(--primary-8)] bg-[var(--primary-7)]'
                  : 'border-[var(--divider-color)] hover:border-[var(--primary-8)]'
              "
              @click="selectEdition(edition.code)"
            >
              <n-radio :checked="targetEdition === edition.code" @update:checked="selectEdition(edition.code)" />
              <div class="flex flex-1 items-center justify-between">
                <span class="font-medium text-[var(--text-n1)]">{{ edition.name }}</span>
                <span class="text-[13px] text-[var(--text-n3)]">
                  {{ editionFeatureLabel(edition) }}
                </span>
                <span class="text-orange-500">¥{{ edition.yearPrice ?? 0 }}/年</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 报价 -->
        <div v-if="quote" class="flex items-start gap-[12px]">
          <span class="w-[90px] shrink-0 pt-[6px] text-right text-[var(--text-n4)]">
            {{ t('system.personal.renew.quote') }}
          </span>
          <div class="flex flex-1 flex-col gap-[4px]">
            <div class="flex items-center gap-[8px]">
              <n-tag :type="quoteTypeTag" size="small" :bordered="false">{{ quoteTypeLabel }}</n-tag>
              <span class="text-lg font-medium text-orange-500">¥{{ fmtMoney(quote.amount) }}</span>
            </div>
            <div v-if="quote.priceDetail" class="text-[13px] text-[var(--text-n3)]">{{ quote.priceDetail }}</div>
          </div>
        </div>

        <!-- 付款方式 -->
        <div class="flex items-center gap-[12px]">
          <span class="w-[90px] shrink-0 text-right text-[var(--text-n4)]">
            {{ t('system.personal.renew.paymentType') }}
          </span>
          <n-radio-group v-model:value="form.paymentType">
            <n-radio-button v-for="opt in paymentTypeOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
          </n-radio-group>
        </div>

        <!-- 微信/支付宝收款码 -->
        <div v-if="isQrPayment" class="flex items-start gap-[12px]">
          <span class="w-[90px] shrink-0 pt-[6px] text-right text-[var(--text-n4)]">
            {{ t('system.personal.renew.wechatQrcode') }}
          </span>
          <div class="flex flex-1 flex-col gap-[8px]">
            <n-image
              v-if="qrcodeUrl"
              :src="qrcodeUrl"
              :width="180"
              :height="180"
              object-fit="contain"
              class="rounded border border-[var(--divider-color)]"
            />
            <div v-else class="text-[13px] text-orange-500">{{ t('system.personal.renew.qrcodeMissing') }}</div>
            <div class="text-[13px] text-[var(--text-n3)]">{{ t('system.personal.renew.wechatQrcodeHint') }}</div>
          </div>
        </div>

        <!-- 对公转账账户信息 -->
        <div
          v-if="form.paymentType === PlatformPaymentTypeEnum.TRANSFER && activePaymentAccount"
          class="flex items-start gap-[12px]"
        >
          <span class="w-[90px] shrink-0 pt-[6px] text-right text-[var(--text-n4)]">
            {{ t('system.personal.renew.paymentType.TRANSFER') }}
          </span>
          <div class="flex flex-1 flex-col gap-[4px] text-[13px] text-[var(--text-n3)]">
            <div v-if="activePaymentAccount.accountName">
              {{ t('system.personal.renew.accountName') }}：{{ activePaymentAccount.accountName }}
            </div>
            <div v-if="activePaymentAccount.accountNo">
              {{ t('system.personal.renew.accountNo') }}：{{ activePaymentAccount.accountNo }}
            </div>
            <div v-if="activePaymentAccount.bankName">
              {{ t('system.personal.renew.bankName') }}：{{ activePaymentAccount.bankName }}
            </div>
          </div>
        </div>

        <!-- 付款凭证 -->
        <div class="flex items-start gap-[12px]">
          <span class="w-[90px] shrink-0 pt-[6px] text-right text-[var(--text-n4)]">
            {{ t('system.personal.renew.voucher') }}
          </span>
          <div class="flex flex-1 flex-col gap-[8px]">
            <n-upload
              v-model:file-list="voucherFileList"
              :multiple="false"
              :custom-request="uploadVoucher"
              @remove="removeVoucher"
            >
              <n-button>{{ t('system.personal.renew.uploadVoucher') }}</n-button>
            </n-upload>
          </div>
        </div>

        <!-- 备注 -->
        <div class="flex items-center gap-[12px]">
          <span class="w-[90px] shrink-0 text-right text-[var(--text-n4)]">
            {{ t('system.personal.renew.remark') }}
          </span>
          <n-input v-model:value="form.remark" class="flex-1" type="textarea" :rows="2" />
        </div>
      </div>
    </n-scrollbar>

    <template #footer>
      <div class="flex w-full flex-col gap-[8px]">
        <div class="text-right text-[12px] text-[var(--text-n3)]">
          {{ t('system.personal.renew.agreementHint') }}
          <a class="cursor-pointer text-[var(--primary)]" @click="openAgreement('service')">
            {{ t('system.personal.renew.agreementService') }}
          </a>
        </div>
        <div class="flex w-full items-center justify-end">
          <n-button secondary class="mx-[8px]" @click="cancel">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="submitting" :disabled="!targetEdition" @click="submit">
            {{ t('system.personal.renew.submit') }}
          </n-button>
        </div>
      </div>
    </template>
  </CrmModal>

  <AgreementModal v-model:show="showAgreement" v-model:active-key="agreementActiveKey" />
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import {
    NButton,
    NImage,
    NInput,
    NRadio,
    NRadioButton,
    NRadioGroup,
    NScrollbar,
    NTag,
    NUpload,
    useMessage,
  } from 'naive-ui';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { PlatformPaymentTypeEnum } from '@lib/shared/enums/platformFinanceEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { Edition } from '@lib/shared/models/system/edition';
  import type { PlatformBankAccount } from '@lib/shared/models/system/platformFinance';
  import type { TenantPlanQuote } from '@lib/shared/models/system/tenant-plan';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import AgreementModal from '@/components/agreement/AgreementModal.vue';

  import {
    applyPlan,
    getPlanEditions,
    getPlanPaymentAccounts,
    getPlanQuote,
    getSubscription,
    uploadTempAttachment,
  } from '@/api/modules';
  import { useUserStore } from '@/store';

  import type { UploadCustomRequestOptions, UploadFileInfo } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const emit = defineEmits<{
    (e: 'success'): void;
  }>();

  const editions = ref<Edition[]>([]);
  const bankAccounts = ref<PlatformBankAccount[]>([]);
  const targetEdition = ref<string>('');
  const quote = ref<TenantPlanQuote | null>(null);
  const submitting = ref(false);
  const voucherFileList = ref<UploadFileInfo[]>([]);
  const uploadedVoucherIds = ref<Record<string, string>>({});
  const currentVersion = ref<{ versionName?: string; remainDays?: number; status?: string; inGrace?: boolean }>({});

  const showAgreement = ref(false);
  const agreementActiveKey = ref<'service' | 'ip' | 'privacy'>('service');

  function openAgreement(key: 'service' | 'ip' | 'privacy' = 'service') {
    agreementActiveKey.value = key;
    showAgreement.value = true;
  }

  const form = ref<{ paymentType: string; remark: string }>({
    paymentType: PlatformPaymentTypeEnum.WECHAT,
    remark: '',
  });

  const paymentTypeOptions = [
    { label: t('system.personal.renew.paymentType.WECHAT'), value: PlatformPaymentTypeEnum.WECHAT },
    { label: t('system.personal.renew.paymentType.ALIPAY'), value: PlatformPaymentTypeEnum.ALIPAY },
    { label: t('system.personal.renew.paymentType.TRANSFER'), value: PlatformPaymentTypeEnum.TRANSFER },
    { label: t('system.personal.renew.paymentType.OFFLINE'), value: PlatformPaymentTypeEnum.OFFLINE },
  ];

  const currentLabel = computed(() => {
    const name = currentVersion.value.versionName || quote.value?.currentVersionName;
    if (!name) return t('system.personal.renew.notPurchased');
    const remain = currentVersion.value.remainDays ?? quote.value?.remainDays;
    if (currentVersion.value.inGrace) {
      const days = remain == null ? 0 : Math.abs(remain);
      return `${name}（${t('system.personal.subscription.inGrace')}${days}${t('system.personal.subscription.day')}）`;
    }
    if (currentVersion.value.status === 'FREE') {
      return `${name}-${t('system.personal.subscription.status.FREE')}（${t('system.personal.renew.remainDays')}${
        remain ?? 0
      }${t('system.personal.subscription.day')}）`;
    }
    if (currentVersion.value.status === 'EXPIRED') {
      return `${name}（${t('system.personal.subscription.status.EXPIRED')}）`;
    }
    return `${name}（${t('system.personal.renew.remainDays')}${remain ?? 0}${t('system.personal.subscription.day')}）`;
  });

  const quoteTypeLabel = computed(() => {
    const type = quote.value?.type;
    if (!type) return '';
    return t(`system.personal.renew.type.${type}`);
  });

  const quoteTypeTag = computed(() => {
    switch (quote.value?.type) {
      case 'UPGRADE':
        return 'warning';
      case 'DOWNGRADE':
        return 'default';
      default:
        return 'info';
    }
  });

  const activePaymentAccount = computed(() => bankAccounts.value.find((a) => a.accountType === form.value.paymentType));
  const qrcodeUrl = computed(() => {
    const acc = activePaymentAccount.value;
    return acc?.qrcode ? `${PreviewAttachmentUrl}/${acc.qrcode}?userId=${userStore.userInfo.id}` : '';
  });
  const isQrPayment = computed(() =>
    [PlatformPaymentTypeEnum.WECHAT, PlatformPaymentTypeEnum.ALIPAY].includes(
      form.value.paymentType as PlatformPaymentTypeEnum
    )
  );

  function editionFeatureLabel(edition: Edition) {
    const parts: string[] = [];
    if (edition.aiMonthlyQuota != null) parts.push(`AI ${edition.aiMonthlyQuota}`);
    if (edition.softLimit != null) parts.push(`${edition.softLimit}人`);
    return parts.join(' · ');
  }

  function fmtMoney(v?: number | string) {
    const n = Number(v ?? 0);
    return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  async function selectEdition(code: string) {
    targetEdition.value = code;
    quote.value = null;
    try {
      quote.value = (await getPlanQuote(code)) || null;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function uploadVoucher(options: UploadCustomRequestOptions) {
    try {
      const res = await uploadTempAttachment(options.file.file as File);
      const [attachmentId] = res.data;
      uploadedVoucherIds.value[options.file.id] = attachmentId;
      options.onFinish();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      options.onError();
    }
  }

  function removeVoucher(options: { file: UploadFileInfo }) {
    delete uploadedVoucherIds.value[options.file.id];
  }

  function resetForm() {
    targetEdition.value = '';
    quote.value = null;
    currentVersion.value = {};
    form.value = { paymentType: PlatformPaymentTypeEnum.WECHAT, remark: '' };
    voucherFileList.value = [];
    uploadedVoucherIds.value = {};
  }

  function cancel() {
    showModal.value = false;
    resetForm();
  }

  async function submit() {
    if (!targetEdition.value) {
      Message.warning(t('system.personal.renew.selectVersion'));
      return;
    }
    submitting.value = true;
    try {
      const voucherIds = Object.values(uploadedVoucherIds.value).filter(Boolean).join(',');
      await applyPlan({
        targetEdition: targetEdition.value,
        paymentType: form.value.paymentType,
        voucherIds: voucherIds || undefined,
        remark: form.value.remark.trim() || undefined,
      });
      Message.success(t('system.personal.renew.submitSuccess'));
      showModal.value = false;
      resetForm();
      emit('success');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      submitting.value = false;
    }
  }

  async function loadInitData() {
    // 当前套餐信息（当前版本/剩余天数，打开即展示，不依赖选中目标版本）
    try {
      const sub = await getSubscription();
      currentVersion.value = {
        versionName: sub?.versionName,
        remainDays: sub?.remainDays,
        status: sub?.status,
        inGrace: sub?.inGrace,
      };
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
    // 版本列表与收款账户分开拉取，任一失败不影响另一个
    try {
      editions.value = (await getPlanEditions()) || [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
    try {
      bankAccounts.value = (await getPlanPaymentAccounts()) || [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  watch(
    () => showModal.value,
    async (val) => {
      if (val) {
        resetForm();
        await loadInitData();
      }
    }
  );
</script>
