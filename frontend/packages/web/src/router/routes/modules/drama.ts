import { DramaRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// AI 漫剧（功能 7 扩展）。已封存：仅平台超级管理员（userId == admin）可见，
// 前端 usePermission.accessRouter 隐藏菜单/拦截路由，后端 PlatformSessionUtils.isAdmin() 兜底。
const drama: AppRouteRecordRaw = {
  path: '/ai-drama',
  name: DramaRouteEnum.DRAMA,
  redirect: '/ai-drama/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.aiDrama',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.aiDrama',
  },
  children: [
    {
      path: 'index',
      name: DramaRouteEnum.DRAMA_INDEX,
      component: () => import('@/views/drama/index.vue'),
      meta: {
        locale: 'menu.aiDrama',
        permissions: [],
      },
    },
    {
      path: 'workspace/:projectId',
      name: DramaRouteEnum.DRAMA_WORKSPACE,
      component: () => import('@/views/drama/workspace.vue'),
      meta: {
        locale: 'menu.aiDrama',
        permissions: [],
        hideInMenu: true,
      },
    },
    {
      path: 'episode/:projectId/:episodeNumber',
      name: DramaRouteEnum.DRAMA_EPISODE,
      component: () => import('@/views/drama/episode.vue'),
      meta: {
        locale: 'menu.aiDrama',
        permissions: [],
        hideInMenu: true,
      },
    },
    {
      path: 'assets',
      name: DramaRouteEnum.DRAMA_ASSETS,
      component: () => import('@/views/drama/assets.vue'),
      meta: {
        locale: 'menu.aiDrama',
        permissions: [],
        hideInMenu: true,
      },
    },
  ],
};

export default drama;
