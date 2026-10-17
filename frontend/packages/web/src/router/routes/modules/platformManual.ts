import { PlatformManualRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 管理员手册（平台侧）：把散落在代码里的规则、配置、操作流程整理成「使用说明书」，
// 供平台 admin、城市经理查阅与培训。可见性：admin 由 isAdmin 短路放行，城市经理由
// 角色种子持有 CITY_MANAGER_ORG:READ，普通租户无此权限故不可见。
// 侧边栏只保留一个一级菜单项，六个二级章节收进页面内左侧导航（docs 风格）。
const platformManual: AppRouteRecordRaw = {
  path: '/platformManual',
  name: PlatformManualRouteEnum.PLATFORM_MANUAL,
  redirect: '/platformManual/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.platformManual',
    permissions: ['CITY_MANAGER_ORG:READ'],
    icon: 'iconicon_books',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.platformManual',
  },
  children: [
    {
      path: 'index',
      name: PlatformManualRouteEnum.PLATFORM_MANUAL_INDEX,
      component: () => import('@/views/platformManual/index.vue'),
      meta: {
        locale: 'menu.platformManual',
        permissions: ['CITY_MANAGER_ORG:READ'],
      },
    },
  ],
};

export default platformManual;
