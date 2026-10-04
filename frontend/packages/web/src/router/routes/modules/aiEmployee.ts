import { AiEmployeeRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const aiEmployee: AppRouteRecordRaw = {
  path: '/ai-employee',
  name: AiEmployeeRouteEnum.AI_EMPLOYEE,
  redirect: '/ai-employee/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.aiEmployee',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.aiEmployee',
  },
  children: [
    {
      path: 'index',
      name: AiEmployeeRouteEnum.AI_EMPLOYEE_INDEX,
      component: () => import('@/views/aiEmployee/index.vue'),
      meta: {
        locale: 'menu.aiEmployee',
        permissions: [],
      },
    },
  ],
};

export default aiEmployee;
