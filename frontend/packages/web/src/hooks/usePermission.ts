import { RouteLocationNormalized, RouteRecordRaw } from 'vue-router';

import useAppStore from '@/store/modules/app';
import useUserStore from '@/store/modules/user';
import { hasAnyPermission, topLevelMenuHasPermission } from '@/utils/permission';

import { PendingRouteEnum } from '@/enums/routeEnum';

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
      // 「待完善功能」下的四个 AI 生成功能已封存：仅平台超级管理员（userId == admin）可见/可访问。
      // 直接敲子路由 URL 时 route.name 是叶子名，需按 matched 链命中父级 aiPending 一并拦截。
      const matched = (route as RouteLocationNormalized).matched ?? [];
      if (route.name === PendingRouteEnum.PENDING || matched.some((r) => r.name === PendingRouteEnum.PENDING)) {
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
