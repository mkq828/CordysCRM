import { PptRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const ppt: AppRouteRecordRaw = {
  path: '/ai-ppt',
  name: PptRouteEnum.PPT,
  redirect: '/ai-ppt/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.aiPpt',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.aiPpt',
  },
  children: [
    {
      path: 'index',
      name: PptRouteEnum.PPT_INDEX,
      component: () => import('@/views/ppt/index.vue'),
      meta: {
        locale: 'menu.aiPpt',
        permissions: [],
      },
    },
  ],
};

export default ppt;
