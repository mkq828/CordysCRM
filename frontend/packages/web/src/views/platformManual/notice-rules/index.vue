<template>
  <div class="flex flex-col gap-[16px]">
    <ManualHeader
      :title="t('menu.platformManualNoticeRules')"
      :subtitle="t('platformManualNoticeRules.intro')"
      icon="iconicon_notification"
      color="#4282FF"
    />

    <div class="rounded-[12px] bg-white p-[16px] shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <div class="mb-[14px] flex items-center gap-[8px]">
        <span
          class="flex h-[26px] w-[26px] items-center justify-center rounded-[7px]"
          style="color: #4282ff; background: #4282ff1f"
        >
          <CrmIcon type="iconicon_notification" :size="15" />
        </span>
        <span class="text-[15px] font-semibold text-[#1f2329]">{{ t('platformManualNoticeRules.eventsTitle') }}</span>
      </div>
      <div class="overflow-x-auto rounded-[8px] border border-[#eef0f3]">
        <table class="w-full border-collapse text-[13px] leading-[20px] text-[#1f2329]">
          <thead>
            <tr style="background: #4282ff0d">
              <th class="pm-th w-[200px] text-[#4282ff]">{{ t('platformManualNoticeRules.col.event') }}</th>
              <th class="pm-th text-[#4282ff]">{{ t('platformManualNoticeRules.col.trigger') }}</th>
              <th class="pm-th w-[160px] text-[#4282ff]">{{ t('platformManualNoticeRules.col.receiver') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(event, idx) in events" :key="event.key" :class="idx % 2 === 1 ? 'bg-[#fafbfc]' : 'bg-white'">
              <td class="pm-td align-top">
                <span class="flex items-center gap-[8px] font-medium">
                  <span
                    class="inline-block h-[6px] w-[6px] shrink-0 rounded-full"
                    :style="{ background: receiverColor[event.receiverType] }"
                  ></span>
                  {{ t(`platformManualNoticeRules.event.${event.key}.name`) }}
                </span>
              </td>
              <td class="pm-td align-top text-[#6b7280]">{{
                t(`platformManualNoticeRules.event.${event.key}.trigger`)
              }}</td>
              <td class="pm-td align-top">
                <span
                  class="inline-block rounded-[6px] px-[8px] py-[2px] text-[12px] font-medium"
                  :style="{
                    background: `${receiverColor[event.receiverType]}1a`,
                    color: receiverColor[event.receiverType],
                  }"
                >
                  {{ t(`platformManualNoticeRules.event.${event.key}.receiver`) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="rounded-[12px] bg-white p-[16px] shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <div class="mb-[14px] flex items-center gap-[8px]">
        <span
          class="flex h-[26px] w-[26px] items-center justify-center rounded-[7px]"
          style="color: #f59e0b; background: #f59e0b1f"
        >
          <CrmIcon type="iconicon_notification" :size="15" />
        </span>
        <span class="text-[15px] font-semibold text-[#1f2329]">{{ t('platformManualNoticeRules.specialTitle') }}</span>
      </div>
      <div class="flex flex-col gap-[10px]">
        <div
          v-for="(special, idx) in specials"
          :key="special.key"
          class="relative overflow-hidden rounded-[10px] border bg-[#fffbeb] p-[14px]"
          style="border-color: #fde68a"
        >
          <span class="absolute left-0 top-0 h-full w-[3px]" style="background: #f59e0b"></span>
          <div class="flex items-start gap-[10px] pl-[6px]">
            <span
              class="mt-[1px] flex h-[20px] w-[20px] shrink-0 items-center justify-center rounded-[6px] text-[12px] font-semibold"
              style="color: #f59e0b; background: #f59e0b1f"
            >
              {{ idx + 1 }}
            </span>
            <div class="text-[13px] leading-[20px] text-[#6b7280]">
              {{ t(`platformManualNoticeRules.special.${special.key}`) }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import ManualHeader from '@/views/platformManual/components/ManualHeader.vue';

  const { t } = useI18n();

  const receiverColor: Record<string, string> = {
    admin: '#4282FF',
    applicant: '#06b6d4',
    tenant: '#8b5cf6',
  };

  const events = [
    { key: 'registerApply', receiverType: 'admin' },
    { key: 'registerApproved', receiverType: 'applicant' },
    { key: 'registerRejected', receiverType: 'applicant' },
    { key: 'planExpire', receiverType: 'tenant' },
    { key: 'planApply', receiverType: 'admin' },
    { key: 'planApproved', receiverType: 'tenant' },
    { key: 'planRejected', receiverType: 'tenant' },
    { key: 'quotaBreak', receiverType: 'tenant' },
    { key: 'paymentVerified', receiverType: 'tenant' },
  ];

  const specials = [{ key: 'paymentNoSms' }, { key: 'renewalNoDup' }, { key: 'personalNoAudit' }];
</script>

<style lang="less" scoped>
  .pm-th {
    padding: 10px 12px;
    font-weight: 600;
    border-bottom: 1px solid #eef0f3;
    text-align: left;
    white-space: nowrap;
  }
  .pm-td {
    padding: 10px 12px;
    border-bottom: 1px solid #eef0f3;
  }
</style>
