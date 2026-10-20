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
const EFFECTS_ROUTE_NAME = 'effects';
const SECURITY_EVENT_ROUTE_NAME = 'systemSecurityEvent';

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
      // 「待完善功能」与「视觉特效库」仅平台超级管理员（userId == admin）可见/可访问：
      // 前者是封存的半成品功能，后者是开发时选型特效的预览页，都不对普通用户开放。
      // 直接敲子路由 URL 时 route.name 是叶子名，需按 matched 链命中父级一并拦截。
      const matched = (route as RouteLocationNormalized).matched ?? [];
      const adminOnly = [PENDING_ROUTE_NAME, EFFECTS_ROUTE_NAME, SECURITY_EVENT_ROUTE_NAME].some(
        (n) => route.name === n || matched.some((r) => r.name === n)
      );
      if (adminOnly) {
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
