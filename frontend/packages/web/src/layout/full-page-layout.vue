<template>
  <n-layout class="layout">
    <router-view v-slot="{ Component, route }">
      <div class="page-content">
        <component :is="Component" :key="route.fullPath" />
      </div>
    </router-view>
  </n-layout>
</template>

<script setup lang="ts">
  import { NLayout } from 'naive-ui';
</script>

<style lang="less" scoped>
  .layout {
    width: 100%;
    height: 100vh;
    .page-content {
      @apply overflow-y-auto;

      height: 100%;
      background-color: var(--color-bg-3);
      .crm-scroll-bar();
    }
  }
</style>
