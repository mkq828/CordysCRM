import { SuggestionRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const suggestion: AppRouteRecordRaw = {
  path: '/suggestion',
  name: SuggestionRouteEnum.SUGGESTION,
  redirect: '/suggestion/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.suggestion',
    permissions: [],
    icon: 'iconicon_user_talk',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.suggestion',
  },
  children: [
    {
      path: 'index',
      name: SuggestionRouteEnum.SUGGESTION_INDEX,
      component: () => import('@/views/suggestion/index.vue'),
      meta: {
        locale: 'menu.suggestion',
        isTopMenu: true,
        permissions: [],
      },
    },
    {
      path: 'detail/:id',
      name: SuggestionRouteEnum.SUGGESTION_DETAIL,
      component: () => import('@/views/suggestion/detail.vue'),
      meta: {
        locale: 'menu.suggestion',
        permissions: [],
      },
    },
  ],
};

export default suggestion;
