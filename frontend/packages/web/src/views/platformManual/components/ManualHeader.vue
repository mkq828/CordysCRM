<template>
  <div class="relative rounded-[16px] p-[24px]" :style="headerStyle">
    <div class="pointer-events-none absolute inset-0 overflow-hidden rounded-[16px]">
      <div
        class="absolute -right-[40px] -top-[40px] h-[150px] w-[150px] rounded-full opacity-[0.14]"
        :style="{ background: color }"
      ></div>
      <div
        class="absolute -bottom-[60px] right-[120px] h-[120px] w-[120px] rounded-full opacity-[0.08]"
        :style="{ background: color }"
      ></div>
    </div>
    <div class="relative flex items-center gap-[16px]">
      <div
        class="flex h-[52px] w-[52px] shrink-0 items-center justify-center rounded-[14px] leading-none"
        :style="iconBadgeStyle"
      >
        <CrmIcon :type="icon" :size="26" color="#ffffff" />
      </div>
      <div class="min-w-0 flex-1">
        <div class="text-[18px] font-semibold leading-[26px] text-[#1f2329]">{{ title }}</div>
        <div v-if="subtitle" class="mt-[6px] text-[13px] leading-[20px] text-[#6b7280]">{{ subtitle }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  const props = withDefaults(
    defineProps<{
      title: string;
      subtitle?: string;
      icon: string;
      color?: string;
    }>(),
    { color: '#4282FF', subtitle: '' }
  );

  const headerStyle = computed(() => ({
    background: `linear-gradient(135deg, ${props.color}1f 0%, ${props.color}0a 55%, #ffffff 100%)`,
    border: `1px solid ${props.color}33`,
  }));

  const iconBadgeStyle = computed(() => ({
    background: `linear-gradient(135deg, ${props.color}, ${props.color}dd)`,
    boxShadow: `0 4px 14px ${props.color}40`,
  }));
</script>
