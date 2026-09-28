import { PlatformAiQuotaRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// AI 额度管理（平台端）：仅 admin 可见（未写入角色权限种子，admin 短路放行）。
const platformAiQuota: AppRouteRecordRaw = {
  path: '/platformAiQuota',
  name: PlatformAiQuotaRouteEnum.PLATFORM_AI_QUOTA,
  redirect: '/platformAiQuota/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.platformAiQuota',
    permissions: ['ADMIN_AI_QUOTA:READ'],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.platformAiQuota',
  },
  children: [
    {
      path: 'index',
      name: PlatformAiQuotaRouteEnum.PLATFORM_AI_QUOTA_INDEX,
      component: () => import('@/views/platformAiQuota/index.vue'),
      meta: {
        locale: 'menu.platformAiQuota',
        permissions: ['ADMIN_AI_QUOTA:READ'],
      },
    },
  ],
};

export default platformAiQuota;
