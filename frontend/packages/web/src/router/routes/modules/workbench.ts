// import useLicenseStore from '@/store/modules/setting/license';

import { WorkbenchRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const workbench: AppRouteRecordRaw = {
  path: '/workbench',
  name: WorkbenchRouteEnum.WORKBENCH,
  redirect: {
    name: WorkbenchRouteEnum.WORKBENCH_SMART,
  },
  component: DEFAULT_LAYOUT,
  meta: {
    hideChildrenInMenu: true,
    locale: 'menu.workbench',
    permissions: [],
    icon: 'iconicon_home',
    collapsedLocale: 'menu.workbench',
  },
  children: [
    {
      path: 'smart',
      name: WorkbenchRouteEnum.WORKBENCH_SMART,
      component: () => import('@/views/workbench/smart/index.vue'),
      meta: {
        locale: 'menu.workbench.smart',
        permissions: [],
        isTopMenu: true,
      },
    },
    {
      path: 'index',
      name: WorkbenchRouteEnum.WORKBENCH_BOARD,
      component: () => import('@/views/workbench/index.vue'),
      meta: {
        locale: 'menu.workbench.board',
        permissions: [],
        isTopMenu: true,
      },
    },
    {
      path: 'callReview',
      name: WorkbenchRouteEnum.WORKBENCH_CALL_REVIEW,
      component: () => import('@/views/workbench/callReview/index.vue'),
      meta: {
        locale: 'menu.workbench.callReview',
        permissions: [],
        isTopMenu: true,
      },
    },
  ],
};

export default workbench;
