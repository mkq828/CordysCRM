<template>
  <CrmModal v-model:show="show" size="large" preset="card" :title="t('system.personal.renewApplication.detail')">
    <n-scrollbar style="max-height: 62vh">
      <n-descriptions v-if="application" label-placement="left" :column="2" bordered size="small">
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
          {{
            application.validityDays == null
              ? '-'
              : `${application.validityDays}${t('system.personal.subscription.day')}`
          }}
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

        <!-- 收款账户：租户据此核对是否打错款 -->
        <n-descriptions-item :label="t('planApplication.accountInfo')" :span="2">
          <div
            v-if="application.accountName || application.accountNo || application.bankName"
            class="flex flex-col gap-[6px]"
          >
            <div v-if="application.accountName" class="text-[13px]">
              {{ t('planApplication.accountName') }}：{{ application.accountName }}
            </div>
            <div v-if="application.accountNo" class="text-[13px]">
              {{ t('planApplication.accountNo') }}：{{ application.accountNo }}
            </div>
            <div v-if="application.bankName" class="text-[13px]">
              {{ t('planApplication.bankName') }}：{{ application.bankName }}
            </div>
            <n-image
              v-if="qrcodeUrl"
              :src="qrcodeUrl"
              width="160"
              height="160"
              object-fit="contain"
              class="rounded border border-[var(--divider-color)]"
            />
          </div>
          <span v-else class="text-[13px] text-[var(--text-n4)]">-</span>
        </n-descriptions-item>

        <!-- 付款凭证 -->
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
        <n-descriptions-item v-if="application.verifyRemark" :label="t('planApplication.verifyRemark')" :span="2">
          <span class="text-orange-500">{{ application.verifyRemark }}</span>
        </n-descriptions-item>
      </n-descriptions>
    </n-scrollbar>
  </CrmModal>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { NDescriptions, NDescriptionsItem, NImage, NScrollbar } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { TenantPlanApplicationItem } from '@lib/shared/models/system/tenant-plan';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { useUserStore } from '@/store';

  const { t } = useI18n();
  const userStore = useUserStore();

  const show = defineModel<boolean>('show', { required: true, default: false });

  const props = defineProps<{
    application: TenantPlanApplicationItem | null;
  }>();

  const qrcodeUrl = computed(() =>
    props.application?.qrcode
      ? `${PreviewAttachmentUrl}/${props.application.qrcode}?userId=${userStore.userInfo.id}`
      : ''
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
</script>
