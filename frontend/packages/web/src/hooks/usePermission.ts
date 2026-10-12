import { RouteLocationNormalized, RouteRecordRaw } from 'vue-router';

import useAppStore from '@/store/modules/app';
import useUserStore from '@/store/modules/user';
import { hasAnyPermission, topLevelMenuHasPermission } from '@/utils/permission';

import { DramaRouteEnum } from '@/enums/routeEnum';

/**
 * 用户权限
 * @returns 调用方法
 */
export default function usePermission() {
  const appStore = useAppStore();
  const firstLevelMenu = appStore.moduleConfigList.map((item) => item.moduleKey);

  return {
    /**
     * 是否为允许访问的路由
     * @param route 路由信息
     * @returns 是否
     */
    accessRouter(route: RouteLocationNormalized | RouteRecordRaw) {
      // AI 漫剧已封存：仅平台超级管理员（userId == admin）可见/可访问
      if (route.name === DramaRouteEnum.DRAMA) {
        return useUserStore().isAdmin;
      }
      if (firstLevelMenu.includes(route.name as string)) {
        // 一级菜单: 创建项目时 被勾选的模块
        return topLevelMenuHasPermission(route);
      }
      return (
        route.meta?.requiresAuth === false ||
        !route.meta?.permissions ||
        route.meta?.permissions.includes('*') ||
        hasAnyPermission(route.meta?.permissions || [])
      );
    },
  };
}
