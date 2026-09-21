import { FinanceRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const finance: AppRouteRecordRaw = {
  path: '/finance',
  name: FinanceRouteEnum.FINANCE,
  redirect: '/finance/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.finance',
    permissions: ['FINANCE:READ'],
    icon: 'iconicon_money_circle',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.finance',
  },
  children: [
    {
      path: 'index',
      name: FinanceRouteEnum.FINANCE_INDEX,
      component: () => import('@/views/finance/index.vue'),
      meta: {
        locale: 'menu.finance',
        permissions: ['FINANCE:READ'],
      },
    },
  ],
};

export default finance;
