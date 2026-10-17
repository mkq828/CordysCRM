import { PlatformAdminRouteEnum, SystemRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 平台运营（平台侧）：版本套餐 / 系统日志 / 问题反馈。
// 注册审核 / 付费用户 / 续费申请 已并入「租户全生命周期」（platformFinance）菜单。
const platformAdmin: AppRouteRecordRaw = {
  path: '/platformAdmin',
  name: PlatformAdminRouteEnum.PLATFORM_ADMIN,
  redirect: '/platformAdmin/edition',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.platformAdmin',
    permissions: ['PAID_USER:READ', 'OPERATION_LOG:READ', 'BUG_REPORT:READ'],
    icon: 'iconicon_control_platform',
    hideChildrenInMenu: false,
    collapsedLocale: 'menu.platformAdmin',
  },
  children: [
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
