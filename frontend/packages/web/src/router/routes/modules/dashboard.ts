import { DashboardRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const dashboard: AppRouteRecordRaw = {
  path: '/dashboard',
  name: DashboardRouteEnum.DASHBOARD,
  redirect: '/dashboard/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.dashboard',
    permissions: ['DASHBOARD:READ'],
    icon: 'iconicon_dashboard1',
    // 原「仪表盘」已被全屏「经营大屏」取代，菜单里隐藏，仅保留 index 占位跳转经营大屏。
    hideInMenu: true,
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.dashboard',
  },
  children: [
    {
      path: 'index',
      name: DashboardRouteEnum.DASHBOARD_INDEX,
      component: () => import('@/views/dashboard/index.vue'),
      meta: {
        locale: 'menu.dashboard',
        permissions: ['DASHBOARD:READ'],
      },
    },
  ],
};

export default dashboard;
