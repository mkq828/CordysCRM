import { computed } from 'vue';
import { RouteRecordNormalized, RouteRecordRaw } from 'vue-router';
import { cloneDeep } from 'lodash-es';

import usePermission from '@/hooks/usePermission';
import appClientMenus from '@/router/app-menus';
import { allMenuRouteMap, featureRouteMap } from '@/router/constants';
import useAppStore from '@/store/modules/app';

import {
  AiQuotaRouteEnum,
  CityManagerRouteEnum,
  DashboardRouteEnum,
  EffectsRouteEnum,
  PendingRouteEnum,
  PlatformAdminRouteEnum,
  PlatformAiQuotaRouteEnum,
  PlatformFinanceRouteEnum,
  SuggestionRouteEnum,
  SystemRouteEnum,
  TenderRouteEnum,
} from '@/enums/routeEnum';

// 无模块开关的工具类一级菜单显式顺序（排在模块菜单之后；effects/pending 兜底，其余保持原相对顺序）
const UTILITY_MENU_ORDER: string[] = [
  CityManagerRouteEnum.CITY_MANAGER,
  PlatformAdminRouteEnum.PLATFORM_ADMIN,
  PlatformAiQuotaRouteEnum.PLATFORM_AI_QUOTA,
  SuggestionRouteEnum.SUGGESTION,
  EffectsRouteEnum.EFFECTS,
  PendingRouteEnum.PENDING,
];

/**
 * 获取菜单树
 * @returns
 */
export default function useMenuTree() {
  const appStore = useAppStore();
  const permission = usePermission();

  const menuTree = computed(() => {
    const copyRouter = cloneDeep(appClientMenus) as RouteRecordNormalized[];

    const currentMenuConfig: string[] = appStore.moduleConfigList.filter((e) => e.enable).map((e) => e.moduleKey);

    copyRouter.sort((a: RouteRecordNormalized, b: RouteRecordNormalized) => {
      const getMenuOrder = (route: RouteRecordNormalized) => {
        const name = route.name as string;
        // 「平台大屏」「租户全生命周期」固定在「标讯」一级菜单之后，随标讯模块位置联动
        if (name === DashboardRouteEnum.PLATFORM_DASHBOARD) {
          return currentMenuConfig.indexOf(allMenuRouteMap[TenderRouteEnum.TENDER]) + 0.5;
        }
        if (name === PlatformFinanceRouteEnum.PLATFORM_FINANCE) {
          return currentMenuConfig.indexOf(allMenuRouteMap[TenderRouteEnum.TENDER]) + 0.75;
        }
        const moduleId = Object.keys(allMenuRouteMap).find((key) => name?.includes(key));
        if (moduleId) {
          return currentMenuConfig.indexOf(allMenuRouteMap[moduleId]);
        }
        // 「我的 AI 额度」固定在「系统」一级菜单之前，随系统模块位置联动
        if (name === AiQuotaRouteEnum.AI_QUOTA) {
          return currentMenuConfig.indexOf(allMenuRouteMap[SystemRouteEnum.SYSTEM]) - 0.5;
        }
        // 无模块开关的工具类一级菜单按显式顺序排在模块菜单之后（effects/pending 兜底）
        const utilityIndex = UTILITY_MENU_ORDER.indexOf(name);
        if (utilityIndex >= 0) {
          return 1000 + utilityIndex;
        }
        return 2000;
      };
      return getMenuOrder(a) - getMenuOrder(b);
    });

    function travel(_routes: RouteRecordRaw[], layer: number) {
      if (!_routes) return null;

      const collector = _routes.map((element) => {
        if (element.meta?.hideInMenu === true) {
          return null;
        }

        // 如果是隐藏的模块，则不显示菜单
        const moduleId = Object.keys(featureRouteMap).find((key) => (element.name as string)?.includes(key));
        if (moduleId && featureRouteMap[moduleId] && !currentMenuConfig.includes(featureRouteMap[moduleId])) {
          return null;
        }

        // 权限校验不通过
        if (!permission.accessRouter(element)) {
          return null;
        }
        // TODO license 先放开
        // if (
        //   (!licenseStore.isEnterpriseVersion()) ||
        //   (!licenseStore.hasLicense() && element.name === DashboardRouteEnum.DASHBOARD)
        // ) {
        //   return null;
        // }

        // 叶子菜单
        if (element.meta?.hideChildrenInMenu || !element.children) {
          element.children = [];
          return element;
        }

        // 过滤隐藏的菜单
        element.children = element.children.filter((x) => x.meta?.hideInMenu !== true);

        // 解析子菜单
        const subItem = travel(element.children, layer + 1);

        if (subItem && subItem.length) {
          element.children = subItem as RouteRecordRaw[];
          return element;
        }

        if (layer > 1) {
          element.children = subItem as RouteRecordRaw[];
          return element;
        }

        if (element.meta?.hideInMenu === false) {
          return element;
        }

        return null;
      });
      return collector.filter(Boolean);
    }
    return travel(copyRouter, 0);
  });

  return {
    menuTree,
  };
}
