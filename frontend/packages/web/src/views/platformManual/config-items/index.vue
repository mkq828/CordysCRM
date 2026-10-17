<template>
  <div class="flex flex-col gap-[16px]">
    <ManualHeader
      :title="t('menu.platformManualConfigItems')"
      :subtitle="t('platformManualConfigItems.intro')"
      icon="iconicon_setting"
      color="#06b6d4"
    />

    <ManualSections prefix="platformManualConfigItems" :sections="sections" accent="#06b6d4" />
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import ManualHeader from '@/views/platformManual/components/ManualHeader.vue';
  import ManualSections from '@/views/platformManual/components/ManualSections.vue';

  const { t } = useI18n();

  const sections = [
    {
      key: 'aiModel',
      icon: 'iconicon_setting',
      items: [{ key: 'dualModel' }, { key: 'tenantNoSelf' }],
    },
    { key: 'aiParam', icon: 'iconicon_file', items: [{ key: 'snippet' }] },
    { key: 'internal', icon: 'iconicon_lock_on', items: [{ key: 'allowlist' }] },
    { key: 'verify', icon: 'iconicon_check_circle', items: [{ key: 'wired' }] },
  ];
</script>
