<template>
  <div class="pf-onboarding-flow-page flex h-full flex-col gap-[16px] overflow-y-auto p-[16px]">
    <CrmCard hide-footer>
      <div class="text-[14px] leading-[22px] text-[#4b5563]">
        {{ t('platformFinanceOnboardingFlow.intro') }}
      </div>
    </CrmCard>

    <CrmCard hide-footer>
      <template #title>{{ t('platformFinanceOnboardingFlow.diagram') }}</template>
      <div ref="flowRef" class="pf-onboarding-flow-chart"></div>
    </CrmCard>

    <CrmCard hide-footer>
      <template #title>{{ t('platformFinanceOnboardingFlow.detail') }}</template>
      <div class="overflow-x-auto">
        <table class="w-full border-collapse text-[13px] leading-[20px] text-[#1f2329]">
          <thead>
            <tr>
              <th class="pf-th w-[180px]">{{ t('platformFinanceOnboardingFlow.detail.action') }}</th>
              <th class="pf-th">{{ t('platformFinanceOnboardingFlow.detail.notify') }}</th>
              <th class="pf-th">{{ t('platformFinanceOnboardingFlow.detail.after') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in detailRows" :key="row.key" class="hover:bg-[#fafafa]">
              <td class="pf-td whitespace-nowrap align-top font-medium">
                {{ t(`platformFinanceOnboardingFlow.row.${row.key}.action`) }}
              </td>
              <td class="pf-td align-top">{{ t(`platformFinanceOnboardingFlow.row.${row.key}.notify`) }}</td>
              <td class="pf-td align-top">{{ t(`platformFinanceOnboardingFlow.row.${row.key}.after`) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';

  import mermaid from 'mermaid';

  const { t } = useI18n();

  const flowRef = ref<HTMLDivElement | null>(null);

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
    const node = (key: string) => t(`platformFinanceOnboardingFlow.node.${key}`);
    const edge = (key: string) => t(`platformFinanceOnboardingFlow.edge.${key}`);
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

  async function render() {
    if (!flowRef.value) return;
    try {
      const { svg } = await mermaid.render('pf-onboarding-flow', graphSource());
      flowRef.value.innerHTML = svg;
    } catch (e) {
      console.error('mermaid render failed', e);
    }
  }

  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'strict',
    theme: 'base',
    themeVariables: {
      primaryColor: '#eef4ff',
      primaryBorderColor: '#4282FF',
      primaryTextColor: '#1f2329',
      lineColor: '#9ca3af',
      fontSize: '13px',
      edgeLabelBackground: '#ffffff',
    },
  });

  onMounted(render);
</script>

<style lang="less" scoped>
  .pf-onboarding-flow-page {
    background: #f5f7fa;
  }
  .pf-onboarding-flow-chart {
    display: flex;
    justify-content: center;
    overflow-x: auto;
    width: 100%;
    min-height: 480px;
  }
  .pf-th {
    padding: 10px 12px;
    font-weight: 500;
    border-bottom: 1px solid #e5e7eb;
    text-align: left;
    white-space: nowrap;
    color: #4b5563;
    background: #f5f7fa;
  }
  .pf-td {
    padding: 10px 12px;
    border-bottom: 1px solid #e5e7eb;
  }
</style>
