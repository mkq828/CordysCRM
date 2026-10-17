<template>
  <div class="flex flex-col gap-[16px]">
    <ManualHeader
      :title="t('menu.platformManualOnboardingFlow')"
      :subtitle="t('platformManualOnboardingFlow.intro')"
      icon="iconicon_books"
      color="#4282FF"
    />

    <ManualFlow
      :title="t('platformManualOnboardingFlow.diagram')"
      icon="iconicon_aiming"
      :graph="graphSource()"
      color="#4282FF"
    />

    <div class="rounded-[12px] bg-white p-[16px] shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <div class="mb-[14px] flex items-center gap-[8px]">
        <span
          class="flex h-[26px] w-[26px] items-center justify-center rounded-[7px]"
          style="color: #4282ff; background: #4282ff1f"
        >
          <CrmIcon type="iconicon_root_list" :size="15" />
        </span>
        <span class="text-[15px] font-semibold text-[#1f2329]">{{ t('platformManualOnboardingFlow.detail') }}</span>
      </div>
      <div class="overflow-x-auto rounded-[8px] border border-[#eef0f3]">
        <table class="w-full border-collapse text-[13px] leading-[20px] text-[#1f2329]">
          <thead>
            <tr style="background: #4282ff0d">
              <th class="pm-th w-[180px] text-[#4282ff]">{{ t('platformManualOnboardingFlow.detail.action') }}</th>
              <th class="pm-th text-[#4282ff]">{{ t('platformManualOnboardingFlow.detail.notify') }}</th>
              <th class="pm-th text-[#4282ff]">{{ t('platformManualOnboardingFlow.detail.after') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in detailRows" :key="row.key" :class="idx % 2 === 1 ? 'bg-[#fafbfc]' : 'bg-white'">
              <td class="pm-td whitespace-nowrap align-top font-medium">
                {{ t(`platformManualOnboardingFlow.row.${row.key}.action`) }}
              </td>
              <td class="pm-td align-top text-[#6b7280]">{{
                t(`platformManualOnboardingFlow.row.${row.key}.notify`)
              }}</td>
              <td class="pm-td align-top text-[#6b7280]">{{
                t(`platformManualOnboardingFlow.row.${row.key}.after`)
              }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import ManualFlow from '@/views/platformManual/components/ManualFlow.vue';
  import ManualHeader from '@/views/platformManual/components/ManualHeader.vue';

  const { t } = useI18n();

  const detailRows = [
    { key: 'enterpriseApply' },
    { key: 'personalApply' },
    { key: 'auditApprove' },
    { key: 'auditReject' },
    { key: 'contract' },
    { key: 'paymentCreate' },
    { key: 'paymentWriteOff' },
    { key: 'invoice' },
    { key: 'renewalApply' },
    { key: 'renewalApprove' },
    { key: 'renewalReject' },
  ];

  function graphSource(): string {
    const node = (key: string) => t(`platformManualOnboardingFlow.node.${key}`);
    const edge = (key: string) => t(`platformManualOnboardingFlow.edge.${key}`);
    return [
      'flowchart TD',
      `  A["${node('register')}"] --> B{"${node('audit')}"}`,
      `  B -->|${edge('rejected')}| C["${node('reject')}"]`,
      `  B -->|${edge('approved')}| D["${node('open')}"]`,
      `  D --> E["${node('contract')}"]`,
      `  E --> F["${node('payment')}"]`,
      `  F --> G["${node('writeOff')}"]`,
      `  G --> H["${node('active')}"]`,
      `  H --> I["${node('invoice')}"]`,
      `  H -->|${edge('expired')}| J["${node('expired')}"]`,
      `  J --> K["${node('renewalApply')}"]`,
      `  K --> L["${node('renewalWriteOff')}"]`,
      `  L --> H`,
    ].join('\n');
  }
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
