import { EffectsRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const effects: AppRouteRecordRaw = {
  path: '/effects',
  name: EffectsRouteEnum.EFFECTS,
  redirect: '/effects/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.effects',
    permissions: [],
    icon: 'iconicon_star1',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.effects',
  },
  children: [
    {
      path: 'index',
      name: EffectsRouteEnum.EFFECTS_INDEX,
      component: () => import('@/views/effects/index.vue'),
      meta: {
        locale: 'menu.effects',
        permissions: [],
      },
    },
  ],
};

export default effects;
