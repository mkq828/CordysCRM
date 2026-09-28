import { PlatformFinanceRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 收费管理（平台账）：admin 全量（合同/回款/发票/营收）；city_manager 仅合同+回款（数据范围=自己归属租户）。
// 后端硬校验 + 前端 permission 白名单；permissions 用专属 code，普通角色无法被授权看到。
const platformFinance: AppRouteRecordRaw = {
  path: '/platformFinance',
  name: PlatformFinanceRouteEnum.PLATFORM_FINANCE,
  redirect: '/platformFinance/contract',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.platformFinance',
    permissions: ['ADMIN_FINANCE:READ', 'CITY_MANAGER_CONTRACT:READ', 'CITY_MANAGER_PAYMENT:READ'],
    icon: 'iconicon_money_circle',
    hideChildrenInMenu: false,
    collapsedLocale: 'menu.platformFinance',
  },
  children: [
    {
      path: 'contract',
      name: PlatformFinanceRouteEnum.PLATFORM_FINANCE_CONTRACT,
      component: () => import('@/views/platformFinance/contract/index.vue'),
      meta: {
        locale: 'menu.platformFinanceContract',
        permissions: ['ADMIN_FINANCE:READ', 'CITY_MANAGER_CONTRACT:READ'],
      },
    },
    {
      path: 'payment-record',
      name: PlatformFinanceRouteEnum.PLATFORM_FINANCE_PAYMENT_RECORD,
      component: () => import('@/views/platformFinance/payment-record/index.vue'),
      meta: {
        locale: 'menu.platformFinancePaymentRecord',
        permissions: ['ADMIN_FINANCE:READ', 'CITY_MANAGER_PAYMENT:READ'],
      },
    },
    {
      path: 'invoice',
      name: PlatformFinanceRouteEnum.PLATFORM_FINANCE_INVOICE,
      component: () => import('@/views/platformFinance/invoice/index.vue'),
      meta: {
        locale: 'menu.platformFinanceInvoice',
        permissions: ['ADMIN_FINANCE:READ'],
      },
    },
    {
      path: 'revenue',
      name: PlatformFinanceRouteEnum.PLATFORM_FINANCE_REVENUE,
      component: () => import('@/views/platformFinance/revenue/index.vue'),
      meta: {
        locale: 'menu.platformFinanceRevenue',
        permissions: ['ADMIN_FINANCE:READ'],
      },
    },
  ],
};

export default platformFinance;
