import { defineStore } from 'pinia';

import { useI18n } from '@lib/shared/hooks/useI18n';
import { getGenerateId } from '@lib/shared/method';
import { clearToken, setToken } from '@lib/shared/method/auth';
import { removeRouteListener } from '@lib/shared/method/route-listener';
import type { ApiKeyItem } from '@lib/shared/models/system/business';
import type { LoginParams } from '@lib/shared/models/system/login';
import type { UserInfo } from '@lib/shared/models/user';

import { getApiKeyList, isLogin, login, signout } from '@/api/modules';
import useUser from '@/hooks/useUser';
import router from '@/router';
import { NO_RESOURCE_ROUTE_NAME, NO_RESOURCE_ROUTE_NAME_INDEX } from '@/router/constants';
import useAppStore from '@/store/modules/app/index';
import useLicenseStore from '@/store/modules/setting/license';
import { getFirstRouteNameByPermission, hasAnyPermission } from '@/utils/permission';

export interface UserState {
  loginType: string[];
  userInfo: UserInfo;
  clientIdRandomId: string; // 客户端随机id
  apiKeyList: ApiKeyItem[];
}

const useUserStore = defineStore('user', {
  persist: {
    // planInGrace / planExpireTime 是「本次登录时」的临时状态，由 /is-login、/login 每次实时返回。
    // 持久化会导致：账号从「宽限期」变成「已停用」后，localStorage 里的旧 planInGrace=true 仍然在，
    // 刷新时宽限期弹窗照弹，和「服务已到期」同时出现（两种互斥状态重复提示）。
    // 这里在序列化/反序列化两侧都把这两个字段剥掉，只保留真正需要落盘的字段。
    serializer: {
      serialize(state) {
        const userInfo = state.userInfo as UserInfo;
        const { planInGrace: _g, planExpireTime: _e, ...rest } = userInfo;
        return JSON.stringify({ ...state, userInfo: rest });
      },
      deserialize(value) {
        const parsed = JSON.parse(value);
        if (parsed?.userInfo) {
          const { planInGrace: _g, planExpireTime: _e, ...rest } = parsed.userInfo;
          parsed.userInfo = rest;
        }
        return parsed;
      },
    },
  },
  state: (): UserState => ({
    loginType: [],
    userInfo: {
      id: '',
      name: '',
      email: '',
      password: '',
      enable: true,
      createTime: 0,
      updateTime: 0,
      language: '',
      lastOrganizationId: '',
      phone: '',
      source: '',
      createUser: '',
      updateUser: '',
      platformInfo: '',
      avatar: '',
      permissionIds: [],
      organizationIds: [],
      csrfToken: '',
      sessionId: '',
      roles: [],
      departmentId: '',
      departmentName: '',
      defaultPwd: true,
    },
    clientIdRandomId: '',
    apiKeyList: [],
  }),

  getters: {
    isAdmin(state: UserState) {
      return state.userInfo.id === 'admin';
    },
    isCityManager(state: UserState) {
      return state.userInfo.roles.some((e: any) => e?.id === 'city_manager');
    },
    getScopedValue(state: UserState) {
      const hasAllScopedData = state.userInfo.roles.some((e: any) => e?.dataScope === 'ALL');
      const hasDepScopedData = state.userInfo.roles.some(
        (e: any) => e?.dataScope === 'DEPT_AND_CHILD' || e.dataScope === 'DEPT_CUSTOM'
      );
      if (hasAllScopedData || this.isAdmin) {
        return 'ALL';
      }
      if (hasDepScopedData) {
        return 'DEPARTMENT';
      }
      return 'SELF';
    },
  },
  actions: {
    // 设置用户信息
    setInfo(info: UserInfo) {
      this.$patch({ userInfo: info });
    },
    async login(params: LoginParams) {
      try {
        const res = await login(params);
        setToken(res.sessionId, res.csrfToken);
        useLicenseStore().resetLicenseValidation();
        this.setInfo(res);
        const appStore = useAppStore();
        const lastOrganizationId = res.lastOrganizationId ?? res.organizationIds[0] ?? '';
        this.clientIdRandomId = getGenerateId();
        appStore.setOrgId(lastOrganizationId);
      } catch (error) {
        clearToken();
        throw error;
      }
    },
    // 登出回调
    logoutCallBack() {
      const appStore = useAppStore();
      const licenseStore = useLicenseStore();
      if (!licenseStore.hasLicense()) {
        appStore.resetPageConfig();
      }
      licenseStore.resetLicenseValidation();
      appStore.disconnectSystemMessageSSE();
      // 重置用户信息
      this.$reset();
      clearToken();

      removeRouteListener();
      appStore.hideLoading();
      router.push({ name: 'login' });
    },
    // 登出
    async logout(silence = false) {
      try {
        const { t } = useI18n();
        if (!silence) {
          const appStore = useAppStore();
          appStore.showLoading(t('message.loggingOut'));
        }
        await signout();
      } finally {
        this.logoutCallBack();
      }
    },
    // 获取登录认证方式
    async getAuthentication() {
      try {
        // const res = await getAuthenticationList();
        this.loginType = [];
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
    qrCodeLogin(res: UserInfo) {
      try {
        if (!res) {
          return false;
        }
        setToken(res.sessionId, res.csrfToken);
        useLicenseStore().resetLicenseValidation();
        this.setInfo(res);
        const appStore = useAppStore();
        const lastOrganizationId = res.lastOrganizationId ?? res.organizationIds?.[0] ?? '';
        appStore.setOrgId(lastOrganizationId);
        this.clientIdRandomId = getGenerateId();
        return true;
      } catch (err) {
        // eslint-disable-next-line no-console
        console.log(err);
        clearToken();
        return false;
      }
    },
    async isLogin(isDisabledErrorTip = false) {
      try {
        const res = await isLogin(isDisabledErrorTip);
        if (!res) {
          return false;
        }
        setToken(res.sessionId, res.csrfToken);
        useLicenseStore().resetLicenseValidation();
        this.setInfo(res);
        const appStore = useAppStore();
        const lastOrganizationId = res.lastOrganizationId ?? res.organizationIds?.[0] ?? '';
        appStore.setOrgId(lastOrganizationId);
        return true;
      } catch (err) {
        // eslint-disable-next-line no-console
        console.log(err);
        return false;
      }
    },
    async checkIsLogin(isDisabledErrorTip = false) {
      const { isLoginPage } = useUser();
      const isLoginStatus = await this.isLogin(isDisabledErrorTip);
      if (isLoginStatus) {
        if (isLoginPage()) {
          const currentRouteName = getFirstRouteNameByPermission(router.getRoutes());
          await router.push({ name: currentRouteName });
        } else if (
          router.currentRoute.value.name === NO_RESOURCE_ROUTE_NAME ||
          router.currentRoute.value.name === NO_RESOURCE_ROUTE_NAME_INDEX
        ) {
          // 权限刷新后仍停留在「暂无资源权限」页：通常是路由守卫在 /is-login 刷新权限前
          // 用了 localStorage 里的旧 permissionIds 误判（新增权限点后旧会话缓存过期）。
          // 这里按最新权限纠正到有权限的首页，避免刷新后卡在无权限页。
          const currentRouteName = getFirstRouteNameByPermission(router.getRoutes());
          await router.push({ name: currentRouteName });
        }
      } else if (!isLoginPage()) {
        // 校验失败（token 失效 / 账号硬到期被停用）时同步清掉本地 token 再跳登录页：
        // 否则路由守卫「已登录访问 login」分支会因 token 仍在而把跳转弹回原页面，
        // 导致硬到期后刷新仍停留在工作台。
        clearToken();
        router.push({ name: 'login' });
      }
    },
    async initApiKeyList() {
      if (!hasAnyPermission(['PERSONAL_API_KEY:READ'])) return;
      try {
        const res = await getApiKeyList();
        this.apiKeyList = res.map((item) => ({
          ...item,
          isExpire: item.forever ? false : item.expireTime < Date.now(),
          desensitization: true,
          showDescInput: false,
        }));
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
  },
});

export default useUserStore;
