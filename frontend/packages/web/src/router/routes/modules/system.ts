import { SystemRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const system: AppRouteRecordRaw = {
  path: '/system',
  name: SystemRouteEnum.SYSTEM,
  redirect: '/system/role',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.settings',
    permissions: [
      'SYS_ORGANIZATION:READ',
      'SYSTEM_ROLE:READ',
      'MODULE_SETTING:READ',
      'SYSTEM_NOTICE:READ',
      'SYSTEM_SETTING:READ',
      'OPERATION_LOG:READ',
      'PROCESS_SETTING:READ',
      'SYS_REGISTER_AUDIT:READ',
      'BUG_REPORT:READ',
      'PAID_USER:READ',
    ],
    icon: 'iconicon_set_up',
    collapsedLocale: 'menu.collapsedSettings',
  },
  children: [
    {
      path: 'org',
      name: SystemRouteEnum.SYSTEM_ORG,
      component: () => import('@/views/system/org/index.vue'),
      meta: {
        locale: 'menu.settings.org',
        permissions: ['SYS_ORGANIZATION:READ'],
      },
    },
    {
      path: 'role',
      name: SystemRouteEnum.SYSTEM_ROLE,
      component: () => import('@/views/system/role/index.vue'),
      meta: {
        locale: 'menu.settings.permission',
        permissions: ['SYSTEM_ROLE:READ'],
      },
    },
    {
      path: 'module',
      name: SystemRouteEnum.SYSTEM_MODULE,
      component: () => import('@/views/system/module/index.vue'),
      meta: {
        locale: 'menu.settings.moduleSetting',
        permissions: ['MODULE_SETTING:READ'],
      },
    },
    {
      path: 'message',
      name: SystemRouteEnum.SYSTEM_MESSAGE,
      component: () => import('@/views/system/message/index.vue'),
      meta: {
        locale: 'menu.settings.messageSetting',
        permissions: ['SYSTEM_NOTICE:READ'],
      },
    },
    {
      path: 'process',
      name: SystemRouteEnum.SYSTEM_PROCESS,
      component: () => import('@/views/system/process/process/index.vue'),
      meta: {
        hideChildrenInMenu: true,
        locale: 'menu.settings.processSetting',
        permissions: ['PROCESS_SETTING:READ'],
      },
      children: [
        {
          path: 'process',
          name: SystemRouteEnum.SYSTEM_PROCESS_INDEX,
          component: () => import('@/views/system/process/workflow/index.vue'),
          meta: {
            locale: 'menu.settings.approvalFlow',
            isTopMenu: true,
            permissions: ['PROCESS_SETTING:READ'],
          },
        },
        // todo 这个版本不上工作流
        // {
        //   path: 'workflow',
        //   name: SystemRouteEnum.SYSTEM_PROCESS_WORKFLOW,
        //   component: () => import('@/views/system/process/workflow/index.vue'),
        //   meta: {
        //     locale: 'menu.settings.workflowSetting',
        //     isTopMenu: true,
        //     permissions: [], // todo
        //   },
        // },
      ],
    },
    {
      path: 'business',
      name: SystemRouteEnum.SYSTEM_BUSINESS,
      component: () => import('@/views/system/business/index.vue'),
      meta: {
        locale: 'menu.settings.businessSetting',
        permissions: ['SYSTEM_SETTING:READ'],
      },
    },
    {
      path: 'log',
      name: SystemRouteEnum.SYSTEM_LOG,
      component: () => import('@/views/system/log/index.vue'),
      meta: {
        locale: 'menu.settings.log',
        permissions: ['OPERATION_LOG:READ'],
      },
    },
    {
      path: 'register-audit',
      name: SystemRouteEnum.SYSTEM_REGISTER_AUDIT,
      component: () => import('@/views/system/register-audit/index.vue'),
      meta: {
        locale: 'menu.settings.registerAudit',
        permissions: ['SYS_REGISTER_AUDIT:READ'],
      },
    },
    {
      path: 'bug-report',
      name: SystemRouteEnum.SYSTEM_BUG_REPORT,
      component: () => import('@/views/system/bug-report/index.vue'),
      meta: {
        locale: 'menu.settings.bugReport',
        permissions: ['BUG_REPORT:READ'],
      },
    },
    {
      path: 'paid-user',
      name: SystemRouteEnum.SYSTEM_PAID_USER,
      component: () => import('@/views/system/paid-user/index.vue'),
      meta: {
        locale: 'menu.settings.paidUser',
        permissions: ['PAID_USER:READ'],
      },
    },
    {
      path: 'edition',
      name: SystemRouteEnum.SYSTEM_EDITION,
      component: () => import('@/views/system/edition/index.vue'),
      meta: {
        locale: 'menu.settings.edition',
        permissions: ['PAID_USER:READ'],
      },
    },
  ],
};

export default system;
