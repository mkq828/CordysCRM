<template>
  <div class="flex flex-col gap-[16px]">
    <ManualHeader
      :title="t('menu.platformManualAiFeatures')"
      :subtitle="t('platformManualAiFeatures.intro')"
      icon="iconicon_aiming"
      color="#10b981"
    />

    <ManualFlow
      :title="t('platformManualAiFeatures.flowTitle')"
      icon="iconicon_creditcard"
      :graph="graphSource"
      color="#10b981"
    />

    <ManualSections prefix="platformManualAiFeatures" :sections="sections" accent="#10b981" />
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
    '  A["调用 AI 功能"] --> B{"还有月配额？"}',
    '  B -->|"有"| C["扣月配额"]',
    '  B -->|"用完"| D{"还有额度包？"}',
    '  D -->|"有"| E["扣额度包（具体额度优先）"]',
    '  D -->|"没有"| F["提示升级企业版"]',
  ].join('\n');

  const sections = [
    {
      key: 'release',
      icon: 'iconicon_aiming',
      items: [{ key: 'trial' }, { key: 'featureCode' }, { key: 'model' }],
    },
    {
      key: 'quota',
      icon: 'iconicon_creditcard',
      items: [{ key: 'monthFirst' }, { key: 'exhaust' }],
    },
    {
      key: 'sealed',
      icon: 'iconicon_lock_on',
      items: [{ key: 'pending' }, { key: 'agentAdd' }],
    },
  ];
</script>
