<template>
  <div />
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { BusinessScreenRouteEnum } from '@/enums/routeEnum';

  // 原「仪表盘」占位页已被全屏「经营大屏」取代，此文件仅保留为兼容历史 /dashboard/index 入口，直接跳转大屏。
  // DataEase 看板相关代码（link.vue / module.vue）暂保留未删除。
  const router = useRouter();
  router.replace({ name: BusinessScreenRouteEnum.BUSINESS_SCREEN_INDEX });
</script>
