<template>
  <div class="mb-[14px] flex items-center gap-[8px]">
    <span
      class="flex h-[26px] w-[26px] items-center justify-center rounded-[7px]"
      :style="{ background: `${color}1f`, color }"
    >
      <CrmIcon :type="icon" :size="15" />
    </span>
    <span class="text-[15px] font-semibold text-[#1f2329]">{{ text }}</span>
  </div>
</template>

<script setup lang="ts">
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  defineProps<{
    icon: string;
    color: string;
    text: string;
  }>();
</script>
