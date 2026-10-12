<template>
  <div class="mb-[24px] mt-[8px]">
    <div
      v-if="props.fieldConfig.showLabel"
      class="mb-[8px] flex cursor-pointer select-none items-center gap-[4px]"
      :style="{
        color: fieldConfig.titleColor,
      }"
      @click="emit('toggle')"
    >
      <CrmIcon
        type="iconicon_chevron_down"
        :size="14"
        class="transition-transform duration-200"
        :class="{ '-rotate-90': props.collapsed }"
      />
      <span class="font-semibold">{{ props.fieldConfig.name }}</span>
    </div>
    <n-divider
      class="!mb-[4px] !mt-0"
      :class="props.fieldConfig.dividerClass"
      :dashed="props.fieldConfig.dividerClass === 'divider--dashed'"
    />
    <div v-if="props.fieldConfig.description" class="text-[12px] leading-[20px] text-[var(--text-n4)]">
      {{ props.fieldConfig.description }}
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NDivider } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    collapsed?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'toggle'): void;
  }>();

  // v-bind内必须全小写
  const dividercolor = computed(() => {
    return props.fieldConfig.dividerColor || 'var(--text-n4)';
  });
</script>

<style lang="less" scoped>
  .divider--hidden {
    @apply hidden;
  }
  .divider--double {
    :deep(.n-divider__line) {
      height: 2px;
    }
  }
  :deep(.n-divider__line) {
    background-color: v-bind(dividercolor) !important;
  }
</style>
