<template>
  <ManualGuide
    :title="title"
    :intro="t(`${prefix}.intro`)"
    :icon="meta.icon"
    :color="meta.color"
    :screenshot="meta.screenshot"
    :screenshot-caption="t(`${prefix}.screenshotCaption`)"
    :what="t(`${prefix}.what`)"
    :where="t(`${prefix}.where`)"
    :steps="steps"
    :example="t(`${prefix}.example`)"
    :tip="tip"
  />
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import ManualGuide from '@/views/platformManual/components/ManualGuide.vue';

  import type { GuideMeta } from '@/views/platformManual/usage/guideMeta';

  const props = defineProps<{
    meta: GuideMeta;
  }>();

  const { t } = useI18n();

  const prefix = computed(() => `platformManualUsage.${props.meta.key}`);
  const title = computed(() => t(props.meta.label));

  // 步骤按 step1..step6 取，取不到（t 返回原 key）就停，支持每页不同步数。
  const steps = computed(() => {
    const list: string[] = [];
    for (let i = 1; i <= 6; i += 1) {
      const key = `${prefix.value}.step${i}`;
      const text = t(key);
      if (text === key) break;
      list.push(text);
    }
    return list;
  });

  const tip = computed(() => {
    const key = `${prefix.value}.tip`;
    const text = t(key);
    return text === key ? '' : text;
  });
</script>
