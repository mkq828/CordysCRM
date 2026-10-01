import { CityManagerRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 城市合伙人（平台角色）：admin 走账号管理 + 业绩看板；city_manager 只走业绩看板。
// permissions 用专属 code（未写入后端权限种子），admin 短路可见；city_manager 命中 dashboard。
const cityManager: AppRouteRecordRaw = {
  path: '/cityManager',
  name: CityManagerRouteEnum.CITY_MANAGER,
  redirect: '/cityManager/dashboard',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.cityManager',
    permissions: ['CITY_MANAGER:MANAGE', 'CITY_MANAGER_DASHBOARD:READ'],
    icon: 'iconicon_usergroup',
    hideChildrenInMenu: false,
    collapsedLocale: 'menu.cityManager',
  },
  children: [
    {
      path: 'account',
      name: CityManagerRouteEnum.CITY_MANAGER_ACCOUNT,
      component: () => import('@/views/cityManager/account/index.vue'),
      meta: {
        locale: 'menu.cityManagerAccount',
        permissions: ['CITY_MANAGER:MANAGE'],
      },
    },
    {
      path: 'dashboard',
      name: CityManagerRouteEnum.CITY_MANAGER_DASHBOARD,
      component: () => import('@/views/cityManager/dashboard/index.vue'),
      meta: {
        locale: 'menu.cityManagerDashboard',
        permissions: ['CITY_MANAGER_DASHBOARD:READ'],
      },
    },
  ],
};

export default cityManager;
