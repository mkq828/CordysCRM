import { DashboardRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 平台大屏（仅 admin）：后端硬校验 + 前端 permission 白名单；permissions 用专属 code，
// 该 code 未写入后端权限种子，故不会出现在「角色权限表」里，普通角色永远无法被授权看到。
const platformDashboard: AppRouteRecordRaw = {
  path: '/platformDashboard',
  name: DashboardRouteEnum.PLATFORM_DASHBOARD,
  redirect: '/platformDashboard/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.platformDashboard',
    permissions: ['ADMIN_DASHBOARD:READ'],
    icon: 'iconicon_dashboard1',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.platformDashboard',
  },
  children: [
    {
      path: 'index',
      name: DashboardRouteEnum.PLATFORM_DASHBOARD_INDEX,
      component: () => import('@/views/dashboard/platform/index.vue'),
      meta: {
        locale: 'menu.platformDashboard',
        isTopMenu: true,
        permissions: ['ADMIN_DASHBOARD:READ'],
      },
    },
  ],
};

export default platformDashboard;
