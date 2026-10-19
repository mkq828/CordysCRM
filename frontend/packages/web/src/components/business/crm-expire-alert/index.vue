<template>
  <div v-if="licenseStore.expiredDuring" class="mb-[16px]" :class="props.styleClass">
    <n-alert :type="licenseStore.expiredDays > 0 && licenseStore.expiredDays <= 30 ? 'warning' : 'error'">
      <template #icon>
        <n-icon><AlertCircle /></n-icon>
      </template>
      {{
        licenseStore.expiredDays > 0 && licenseStore.expiredDays <= 30
          ? t('system.license.LicenseExpirationPromptLessThanThirty', { day: licenseStore.expiredDays })
          : getInvalidTip
      }}
    </n-alert>
  </div>
</template>

<script setup lang="ts">
  import { NAlert, NIcon } from 'naive-ui';
  import { AlertCircle } from '@vicons/ionicons5';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import useLicenseStore from '@/store/modules/setting/license';

  const licenseStore = useLicenseStore();
  const { t } = useI18n();

  const props = defineProps<{
    styleClass?: string[];
  }>();

  // 已过期时统一提示续费（原 license 的 invalid/expired 分支已随订阅体系消失）
  const getInvalidTip = computed(() => t('system.license.LicenseExpirationPromptGreaterThanThirty'));
</script>

<style scoped></style>
