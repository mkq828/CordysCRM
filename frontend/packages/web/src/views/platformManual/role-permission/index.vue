<template>
  <div class="flex flex-col gap-[16px]">
    <ManualHeader
      :title="t('menu.platformManualRolePermission')"
      :subtitle="t('platformManualRolePermission.intro')"
      icon="iconicon_usergroup"
      color="#8b5cf6"
    />

    <ManualSections prefix="platformManualRolePermission" :sections="sections" accent="#8b5cf6" />
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import ManualHeader from '@/views/platformManual/components/ManualHeader.vue';
  import ManualSections from '@/views/platformManual/components/ManualSections.vue';

  const { t } = useI18n();

  const sections = [
    {
      key: 'roles',
      icon: 'iconicon_usergroup',
      items: [
        { key: 'admin', tag: 'admin' },
        { key: 'orgAdmin', tag: 'org_admin' },
        { key: 'cityManager', tag: 'city_manager' },
        { key: 'salesStaff', tag: 'sales_staff' },
      ],
    },
    { key: 'cache', icon: 'iconicon_refresh', items: [{ key: 'reload' }] },
    { key: 'visible', icon: 'iconicon_aiming', items: [{ key: 'platformMenus' }, { key: 'manual' }] },
  ];
</script>
