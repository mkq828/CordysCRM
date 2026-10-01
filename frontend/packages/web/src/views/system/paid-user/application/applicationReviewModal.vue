<template>
  <CrmModal v-model:show="show" size="large" preset="card" :title="t('planApplication.reviewTitle')" @cancel="close">
    <n-scrollbar style="max-height: 62vh">
      <n-spin :show="loading">
        <n-descriptions v-if="application" label-placement="left" :column="2" bordered size="small">
          <n-descriptions-item :label="t('planApplication.orgName')" :span="2">
            {{ application.orgName || '-' }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('planApplication.currentVersion')">
            {{ application.currentVersionName || application.currentVersion || '-' }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('planApplication.targetVersion')">
            {{ application.targetVersionName || application.targetVersion || '-' }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('planApplication.amount')">
            <span class="font-medium text-orange-500">{{ fmtMoney(application.amount) }}</span>
          </n-descriptions-item>
          <n-descriptions-item :label="t('planApplication.validityDays')">
            {{ application.validityDays == null ? '-' : `${application.validityDays}${t('paidUser.days')}` }}
          </n-descriptions-item>
          <n-descriptions-item v-if="application.priceDetail" :label="t('planApplication.priceDetail')" :span="2">
            {{ application.priceDetail }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('planApplication.paymentType')">
            {{ paymentTypeLabel(application.paymentType) }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('planApplication.createTime')">
            {{ formatTime(application.createTime) }}
          </n-descriptions-item>

          <!-- 收款账户（admin 在收款设置里配的账户，财务据此核对收款去向） -->
          <n-descriptions-item :label="t('planApplication.accountInfo')" :span="2">
            <div v-if="activeAccount" class="flex flex-col gap-[6px]">
              <div v-if="activeAccount.accountName" class="text-[13px]">
                {{ t('planApplication.accountName') }}：{{ activeAccount.accountName }}
              </div>
              <div v-if="activeAccount.accountNo" class="text-[13px]">
                {{ t('planApplication.accountNo') }}：{{ activeAccount.accountNo }}
              </div>
              <div v-if="activeAccount.bankName" class="text-[13px]">
                {{ t('planApplication.bankName') }}：{{ activeAccount.bankName }}
              </div>
              <n-image
                v-if="isQrPayment && qrcodeUrl"
                :src="qrcodeUrl"
                width="160"
                height="160"
                object-fit="contain"
                class="rounded border border-[var(--divider-color)]"
              />
              <div v-if="isQrPayment && !qrcodeUrl" class="text-[13px] text-orange-500">
                {{ t('planApplication.qrcodeMissing') }}
              </div>
            </div>
            <span v-else class="text-[13px] text-[var(--text-n4)]">-</span>
          </n-descriptions-item>

          <!-- 付款凭证（财务核对收款的关键证据，放大预览） -->
          <n-descriptions-item :label="t('planApplication.voucher')" :span="2">
            <div v-if="voucherIds.length" class="flex flex-wrap gap-[8px]">
              <n-image
                v-for="id in voucherIds"
                :key="id"
                :src="attachmentUrl(id)"
                width="120"
                height="120"
                object-fit="cover"
                class="rounded-[6px] border border-[var(--divider-color)]"
              />
            </div>
            <span v-else class="text-orange-500">{{ t('planApplication.noVoucher') }}</span>
          </n-descriptions-item>

          <n-descriptions-item v-if="application.remark" :label="t('planApplication.remark')" :span="2">
            {{ application.remark }}
          </n-descriptions-item>
        </n-descriptions>
      </n-spin>
    </n-scrollbar>

    <template #footer>
      <div class="flex w-full flex-col gap-[12px]">
        <div class="flex items-start gap-[8px]">
          <span class="w-[72px] shrink-0 pt-[6px] text-right text-[13px] text-[var(--text-n3)]">{{
            t('planApplication.verifyRemark')
          }}</span>
          <n-input
            v-model:value="verifyRemark"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-count
            class="flex-1"
            :placeholder="t('planApplication.verifyRemarkPlaceholder')"
          />
        </div>
        <div class="flex w-full items-center justify-end gap-[12px]">
          <n-button secondary @click="close">{{ t('common.cancel') }}</n-button>
          <n-button type="error" :loading="submitting" @click="handleCancel">{{
            t('planApplication.cancel')
          }}</n-button>
          <n-button type="primary" :loading="submitting" @click="handleApprove">{{
            t('planApplication.approve')
          }}</n-button>
        </div>
      </div>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { NButton, NDescriptions, NDescriptionsItem, NImage, NInput, NScrollbar, NSpin, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { PlatformPaymentTypeEnum } from '@lib/shared/enums/platformFinanceEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { PlatformBankAccount } from '@lib/shared/models/system/platformFinance';
  import type { TenantPlanApplicationItem } from '@lib/shared/models/system/tenant-plan';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { platformBankAccountList, tenantPlanApplicationApprove, tenantPlanApplicationCancel } from '@/api/modules';
  import { useUserStore } from '@/store';

  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  const show = defineModel<boolean>('show', { required: true, default: false });

  const props = defineProps<{
    application: TenantPlanApplicationItem | null;
  }>();

  const emit = defineEmits<{
    (e: 'success'): void;
  }>();

  const loading = ref(false);
  const submitting = ref(false);
  const verifyRemark = ref('');
  const bankAccounts = ref<PlatformBankAccount[]>([]);

  const activeAccount = computed(() =>
    bankAccounts.value.find((a) => a.accountType === props.application?.paymentType)
  );
  const qrcodeUrl = computed(() => {
    const acc = activeAccount.value;
    return acc?.qrcode ? `${PreviewAttachmentUrl}/${acc.qrcode}?userId=${userStore.userInfo.id}` : '';
  });
  const isQrPayment = computed(() =>
    [PlatformPaymentTypeEnum.WECHAT, PlatformPaymentTypeEnum.ALIPAY].includes(
      props.application?.paymentType as PlatformPaymentTypeEnum
    )
  );
  const voucherIds = computed(() =>
    (props.application?.voucherIds || '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
  );

  function paymentTypeLabel(paymentType: string) {
    return paymentType ? t(`planApplication.paymentType.${paymentType}`) : '-';
  }
  function formatTime(ts?: number) {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '-';
  }
  function fmtMoney(v?: number | string) {
    const n = Number(v ?? 0);
    return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  function attachmentUrl(id: string) {
    return `${PreviewAttachmentUrl}/${id}?userId=${userStore.userInfo.id}`;
  }

  function close() {
    show.value = false;
  }

  async function loadBankAccounts() {
    try {
      bankAccounts.value = (await platformBankAccountList()) || [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleApprove() {
    if (!props.application) return;
    submitting.value = true;
    try {
      await tenantPlanApplicationApprove({
        id: props.application.id,
        remark: verifyRemark.value.trim() || undefined,
      });
      Message.success(t('planApplication.approveSuccess'));
      show.value = false;
      emit('success');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      submitting.value = false;
    }
  }

  async function handleCancel() {
    if (!props.application) return;
    const remark = verifyRemark.value.trim();
    if (!remark) {
      Message.warning(t('planApplication.verifyRemarkRequired'));
      return;
    }
    submitting.value = true;
    try {
      await tenantPlanApplicationCancel({ id: props.application.id, remark });
      Message.success(t('planApplication.cancelSuccess'));
      show.value = false;
      emit('success');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      submitting.value = false;
    }
  }

  watch(
    () => show.value,
    (val) => {
      if (val) {
        verifyRemark.value = '';
        bankAccounts.value = [];
        loadBankAccounts();
      }
    }
  );
</script>
