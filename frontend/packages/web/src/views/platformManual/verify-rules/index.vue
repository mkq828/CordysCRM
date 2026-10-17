<template>
  <div class="flex flex-col gap-[16px]">
    <ManualHeader
      :title="t('menu.platformManualVerifyRules')"
      :subtitle="t('platformManualVerifyRules.intro')"
      icon="iconicon_check_circle"
      color="#f59e0b"
    />

    <ManualFlow
      :title="t('platformManualVerifyRules.flowTitle')"
      icon="iconicon_aiming"
      :graph="graphSource"
      color="#f59e0b"
    />

    <ManualSections prefix="platformManualVerifyRules" :sections="sections" accent="#f59e0b" />
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import ManualFlow from '@/views/platformManual/components/ManualFlow.vue';
  import ManualHeader from '@/views/platformManual/components/ManualHeader.vue';
  import ManualSections from '@/views/platformManual/components/ManualSections.vue';

  const { t } = useI18n();

  const graphSource = [
    'flowchart TD',
    '  A["新增回款（金额 = 合同金额）"] --> B["回款核销"]',
    '  B -->|"首笔核销"| C["自动开通企业版"]',
    '  B -->|"撤回核销"| D["只退记账，企业版不关"]',
    '  C --> E["企业版生效（ACTIVE）"]',
    '  E -->|"到期 EXPIRED"| F["续费申请"]',
    '  F --> G["续费核销"]',
    '  G -->|"顺延有效期"| E',
  ].join('\n');

  const sections = [
    {
      key: 'activation',
      icon: 'iconicon_check_circle',
      items: [{ key: 'firstVerify' }, { key: 'revoke' }, { key: 'renewalExtend' }],
    },
    {
      key: 'amount',
      icon: 'iconicon_money_circle',
      items: [{ key: 'fixedAmount' }, { key: 'twoEntries' }],
    },
  ];
</script>
