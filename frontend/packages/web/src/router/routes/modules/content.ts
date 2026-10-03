import { ContentRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const content: AppRouteRecordRaw = {
  path: '/ai-content',
  name: ContentRouteEnum.CONTENT,
  redirect: '/ai-content/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.aiContent',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.aiContent',
  },
  children: [
    {
      path: 'index',
      name: ContentRouteEnum.CONTENT_INDEX,
      component: () => import('@/views/content/index.vue'),
      meta: {
        locale: 'menu.aiContent',
        permissions: [],
      },
    },
  ],
};

export default content;
