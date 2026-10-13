import { RouteLocationNormalized, RouteRecordRaw } from 'vue-router';

import useAppStore from '@/store/modules/app';
import useUserStore from '@/store/modules/user';
import { hasAnyPermission, topLevelMenuHasPermission } from '@/utils/permission';

/**
 * 「待完善功能」父路由名（值 = enums/routeEnum.ts 的 PendingRouteEnum.PENDING）。
 * 刻意不 import 该枚举：mobile 包复用本文件时，其 `@/` 别名指向 mobile 自己的 routeEnum.ts，
 * 那里没有 AI 路由枚举，会导致 mobile typecheck 报 TS2305。
 */
const PENDING_ROUTE_NAME = 'aiPending';

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
      if (route.name === PENDING_ROUTE_NAME || matched.some((r) => r.name === PENDING_ROUTE_NAME)) {
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
