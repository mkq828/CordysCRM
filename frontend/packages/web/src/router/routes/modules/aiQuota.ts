import { AiQuotaRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const aiQuota: AppRouteRecordRaw = {
  path: '/aiQuota',
  name: AiQuotaRouteEnum.AI_QUOTA,
  redirect: '/aiQuota/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.aiQuota',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.aiQuota',
  },
  children: [
    {
      path: 'index',
      name: AiQuotaRouteEnum.AI_QUOTA_INDEX,
      component: () => import('@/views/aiQuota/index.vue'),
      meta: {
        locale: 'menu.aiQuota',
        permissions: [],
      },
    },
  ],
};

export default aiQuota;
