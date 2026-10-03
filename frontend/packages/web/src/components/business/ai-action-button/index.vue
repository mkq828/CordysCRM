<template>
  <n-button type="primary" size="large" :loading="loading" :disabled="disabled" @click="emit('click')">
    <slot />
  </n-button>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  defineProps<{
    /** 加载中（转圈并禁用） */
    loading?: boolean;
    /** 禁用 */
    disabled?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'click'): void;
  }>();
</script>

<style scoped lang="less"></style>
