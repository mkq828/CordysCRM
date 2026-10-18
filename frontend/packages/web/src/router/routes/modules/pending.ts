import { DramaRouteEnum, PendingRouteEnum, ReplicaRouteEnum, VideoRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 「待完善功能」：收编 爆款复刻 / 生成短视频 / AI漫剧 三个暂未打磨好的 AI 生成功能。
// 已封存：仅平台超级管理员（userId == admin）可见/可访问，
// 前端 usePermission.accessRouter 隐藏菜单/拦截路由，后端 PlatformSessionUtils.isAdmin() 兜底。
// 子路由用绝对路径，保持原 URL（如 /ai-video/index）不变，只改菜单归属与访问门槛。
const pending: AppRouteRecordRaw = {
  path: '/ai-pending',
  name: PendingRouteEnum.PENDING,
  redirect: '/ai-replica/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.pending',
    permissions: [],
    icon: 'iconicon_crmbot',
    collapsedLocale: 'menu.pending',
  },
  children: [
    {
      path: '/ai-replica/index',
      name: ReplicaRouteEnum.REPLICA_INDEX,
      component: () => import('@/views/replica/index.vue'),
      meta: {
        locale: 'menu.aiReplica',
        permissions: [],
      },
    },
    {
      path: '/ai-video/index',
      name: VideoRouteEnum.VIDEO_INDEX,
      component: () => import('@/views/video/index.vue'),
      meta: {
        locale: 'menu.aiVideo',
        permissions: [],
      },
    },
    {
      path: '/ai-drama/index',
      name: DramaRouteEnum.DRAMA_INDEX,
      component: () => import('@/views/drama/index.vue'),
      meta: {
        locale: 'menu.aiDrama',
        permissions: [],
      },
    },
    {
      path: '/ai-drama/workspace/:projectId',
      name: DramaRouteEnum.DRAMA_WORKSPACE,
      component: () => import('@/views/drama/workspace.vue'),
      meta: {
        locale: 'menu.aiDrama',
        permissions: [],
        hideInMenu: true,
      },
    },
    {
      path: '/ai-drama/episode/:projectId/:episodeNumber',
      name: DramaRouteEnum.DRAMA_EPISODE,
      component: () => import('@/views/drama/episode.vue'),
      meta: {
        locale: 'menu.aiDrama',
        permissions: [],
        hideInMenu: true,
      },
    },
    {
      path: '/ai-drama/assets',
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

export default pending;
