import { PlatformFinanceRouteEnum, SystemRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 租户全生命周期（平台侧）：注册审核 → 付费用户 → 续费申请 → 合同 → 回款 → 发票 → 营收看板。
// 开通流程总览已挪到「管理员手册」；permissions 用平台专属 code，普通租户角色无法被授权看到。
// city_manager 仅合同 + 回款（数据范围 = 自己归属租户）。
const platformFinance: AppRouteRecordRaw = {
  path: '/platformFinance',
  name: PlatformFinanceRouteEnum.PLATFORM_FINANCE,
  redirect: '/platformFinance/register-audit',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.platformFinance',
    permissions: [
      'ADMIN_FINANCE:READ',
      'SYS_REGISTER_AUDIT:READ',
      'PAID_USER:READ',
      'CITY_MANAGER_CONTRACT:READ',
      'CITY_MANAGER_PAYMENT:READ',
    ],
    icon: 'iconicon_money_circle',
    hideChildrenInMenu: false,
    collapsedLocale: 'menu.platformFinance',
  },
  children: [
    {
      path: 'register-audit',
      name: SystemRouteEnum.SYSTEM_REGISTER_AUDIT,
      component: () => import('@/views/system/register-audit/index.vue'),
      meta: {
        locale: 'menu.platformAdmin.registerAudit',
        permissions: ['SYS_REGISTER_AUDIT:READ'],
      },
    },
    {
      path: 'paid-user',
      name: SystemRouteEnum.SYSTEM_PAID_USER,
      component: () => import('@/views/system/paid-user/index.vue'),
      meta: {
        locale: 'menu.platformAdmin.paidUser',
        permissions: ['PAID_USER:READ'],
      },
    },
    {
      path: 'plan-application',
      name: SystemRouteEnum.SYSTEM_PLAN_APPLICATION,
      component: () => import('@/views/system/paid-user/application/index.vue'),
      meta: {
        locale: 'menu.platformAdmin.planApplication',
        permissions: ['PAID_USER:READ'],
      },
    },
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
