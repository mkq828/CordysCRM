import { BusinessScreenRouteEnum } from '@/enums/routeEnum';

import { FULL_PAGE_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const businessScreen: AppRouteRecordRaw = {
  path: '/business-screen',
  name: BusinessScreenRouteEnum.BUSINESS_SCREEN,
  redirect: '/business-screen/index',
  component: FULL_PAGE_LAYOUT,
  meta: {
    locale: 'menu.businessScreen',
    permissions: ['DASHBOARD:READ'],
    icon: 'iconicon_precise_monitor',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.businessScreen',
  },
  children: [
    {
      path: 'index',
      name: BusinessScreenRouteEnum.BUSINESS_SCREEN_INDEX,
      component: () => import('@/views/businessScreen/index.vue'),
      meta: {
        locale: 'menu.businessScreen',
        permissions: ['DASHBOARD:READ'],
      },
    },
  ],
};

export default businessScreen;
