import { ScriptRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const script: AppRouteRecordRaw = {
  path: '/sales-script',
  name: ScriptRouteEnum.SCRIPT,
  redirect: '/sales-script/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.salesScript',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.salesScript',
  },
  children: [
    {
      path: 'index',
      name: ScriptRouteEnum.SCRIPT_INDEX,
      component: () => import('@/views/script/index.vue'),
      meta: {
        locale: 'menu.salesScript',
        permissions: [],
      },
    },
  ],
};

export default script;
