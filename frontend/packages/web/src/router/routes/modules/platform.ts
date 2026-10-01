import { PlatformAdminRouteEnum, SystemRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 平台运营（平台侧）：注册审核 / 付费用户 / 版本套餐 / 系统日志 / 问题反馈。
// 从「系统」菜单拆出，permissions 用平台专属 code，普通租户角色无法被授权看到。
const platformAdmin: AppRouteRecordRaw = {
  path: '/platformAdmin',
  name: PlatformAdminRouteEnum.PLATFORM_ADMIN,
  redirect: '/platformAdmin/register-audit',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.platformAdmin',
    permissions: ['SYS_REGISTER_AUDIT:READ', 'PAID_USER:READ', 'OPERATION_LOG:READ', 'BUG_REPORT:READ'],
    icon: 'iconicon_control_platform',
    hideChildrenInMenu: false,
    collapsedLocale: 'menu.platformAdmin',
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
      path: 'edition',
      name: SystemRouteEnum.SYSTEM_EDITION,
      component: () => import('@/views/system/edition/index.vue'),
      meta: {
        locale: 'menu.platformAdmin.edition',
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
      path: 'log',
      name: SystemRouteEnum.SYSTEM_LOG,
      component: () => import('@/views/system/log/index.vue'),
      meta: {
        locale: 'menu.platformAdmin.log',
        permissions: ['OPERATION_LOG:READ'],
      },
    },
    {
      path: 'bug-report',
      name: SystemRouteEnum.SYSTEM_BUG_REPORT,
      component: () => import('@/views/system/bug-report/index.vue'),
      meta: {
        locale: 'menu.platformAdmin.bugReport',
        permissions: ['BUG_REPORT:READ'],
      },
    },
  ],
};

export default platformAdmin;
