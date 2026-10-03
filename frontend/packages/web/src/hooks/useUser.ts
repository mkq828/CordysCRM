import { useI18n } from '@lib/shared/hooks/useI18n';

import router from '@/router';
import { NO_RESOURCE_ROUTE_NAME, WHITE_LIST } from '@/router/constants';
import useAppStore from '@/store/modules/app';
import useUserStore from '@/store/modules/user';
import { getFirstRouteNameByPermission, routerNameHasPermission } from '@/utils/permission';

import useDiscreteApi from './useDiscreteApi';

export default function useUser() {
  const { t } = useI18n();

  const logout = async (logoutTo?: string, noRedirect?: boolean, silence = false) => {
    try {
      const userStore = useUserStore();
      await userStore.logout(silence);
      const { message } = useDiscreteApi();
      const currentRoute = router.currentRoute.value;
      if (!silence) {
        message.success(t('message.logoutSuccess'));
        router.push({
          name: logoutTo && typeof logoutTo === 'string' ? logoutTo : 'login',
          query: noRedirect
            ? {}
            : {
                ...router.currentRoute.value.query,
                redirect: currentRoute.name as string,
              },
        });
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  };

  const isLoginPage = () => {
    return window.location.hash.indexOf('login') > -1;
  };

  const isWhiteListPage = () => {
    const currentRoute = router.currentRoute.value;
    return WHITE_LIST.some((e) => e.path.includes(currentRoute.path));
  };

  const goUserHasPermissionPage = async () => {
    const { redirect, ...othersQuery } = router.currentRoute.value.query;

    // 登录后先拉取当前组织的模块配置，避免使用 localStorage 里陈旧的 moduleConfigList（可能是上一个
    // 组织/账号残留、缺首页），导致 getFirstRouteNameByPermission 误判跳回登录页。
    await useAppStore().initModuleConfig();

    const currentRouteName = getFirstRouteNameByPermission(router.getRoutes());
    const redirectHasPermission =
      redirect &&
      !redirect.includes(NO_RESOURCE_ROUTE_NAME) &&
      routerNameHasPermission(redirect as string, router.getRoutes());

    router.push({
      name: redirectHasPermission ? (redirect as string) : currentRouteName,
      query: {
        ...othersQuery,
      },
    });
  };

  return {
    logout,
    isLoginPage,
    isWhiteListPage,
    goUserHasPermissionPage,
  };
}
