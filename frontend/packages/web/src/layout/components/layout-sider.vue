<template>
  <n-layout-sider
    v-model:collapsed="collapsed"
    collapse-mode="width"
    :collapsed-width="56"
    :width="206"
    :native-scrollbar="false"
    class="crm-layout-sider"
    @update-collapsed="appStore.setMenuCollapsed"
  >
    <div class="flex h-full flex-col justify-between">
      <div
        v-if="
          route.name?.toString().includes(DashboardRouteEnum.DASHBOARD) ||
          route.name?.toString().includes(TenderRouteEnum.TENDER)
        "
        :class="`flex justify-center py-[14px] ${appStore.menuCollapsed ? 'px-[16px]' : 'px-[24px]'}`"
      >
        <CrmSvg v-if="appStore.menuCollapsed" name="CORDYS" height="40px" width="40px" />
        <CrmSvg v-else name="logo_CORDYS" height="28px" width="130px" />
      </div>
      <n-scrollbar content-style="min-height: 500px;height: 100%;width: 100%">
        <n-menu
          v-model:value="menuValue"
          v-model:expanded-keys="expandedKeys"
          :root-indent="24"
          :indent="appStore.getMenuIconStatus ? 38 : 8"
          :collapsed-width="appStore.collapsedWidth"
          :icon-size="16"
          :collapsed-icon-size="28"
          :options="menuOptions"
          :render-label="renderLabel"
          :expand-icon="renderExpandMenuIcon"
          accordion
          @update-value="menuChange"
        />
      </n-scrollbar>
      <div class="flex flex-col items-start p-[8px]">
        <n-dropdown
          class="personal-dropdown"
          trigger="hover"
          placement="right-end"
          :options="personalMenuOptions"
          @select="personalMenuChange"
          @update-show="personalMenuUpdateShow"
        >
          <div
            class="personal-info-menu relative flex w-full cursor-pointer items-center gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[8px] hover:bg-[var(--primary-7)]"
            :class="personalMenuShow ? 'bg-[var(--primary-6)]' : ''"
          >
            <CrmPopConfirm
              v-model:show="showPopModal"
              :title="t('system.personal.addNewExport')"
              icon-type="primary"
              :content="t('system.personal.addNewExportPopContent')"
              :positive-text="t('common.gotIt')"
              negative-text=""
              placement="right-end"
              @confirm="confirmHandler"
            >
              <span class="personal-export-pop"></span>
            </CrmPopConfirm>
            <CrmAvatar :size="collapsed ? 25 : 40" class="flex-shrink-0 transition-all" />
            <div v-if="!collapsed" class="one-line-text">
              <div class="one-line-text max-w-[110px]">{{ userStore.userInfo.name }}</div>
              <!-- <n-tag
                v-if="userStore.userInfo.id === 'admin' || (userStore.userInfo.roles[0] as any)?.name"
                :bordered="false"
                size="small"
                :color="{
                  color: 'var(--primary-6)',
                  textColor: 'var(--primary-8)',
                }"
                class="max-w-[110px]"
              >
                {{ userStore.userInfo.id === 'admin' ? t('common.admin') : (userStore.userInfo.roles[0] as any)?.name }}
              </n-tag> -->
            </div>
          </div>
        </n-dropdown>
        <n-divider />
        <div class="ml-[8px] w-full cursor-pointer px-[8px]" @click="() => appStore.setMenuCollapsed(!collapsed)">
          <CrmIcon :type="collapsed ? 'iconicon_menu_fold1' : 'iconicon_menu_unfold1'" :size="16" />
        </div>
      </div>
    </div>
  </n-layout-sider>
  <personalExportDrawer v-model:visible="showPersonalExport" />
</template>

<script setup lang="ts">
  import { RouteLocationNormalizedGeneric, useRoute, useRouter } from 'vue-router';
  import { NDivider, NDropdown, NLayoutSider, NMenu, NScrollbar, NTooltip } from 'naive-ui';

  import { PersonalEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { mapTree } from '@lib/shared/method';
  import { listenerRouteChange } from '@lib/shared/method/route-listener';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';
  import CrmSvg from '@/components/pure/crm-svg/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import personalExportDrawer from '@/views/system/business/components/personalExportDrawer.vue';

  import useMenuTree from '@/hooks/useMenuTree';
  import useUser from '@/hooks/useUser';
  import useVisit from '@/hooks/useVisit';
  import type { AppRouteRecordRaw } from '@/router/routes/types';
  import useAppStore from '@/store/modules/app';
  import useLicenseStore from '@/store/modules/setting/license';
  import useUserStore from '@/store/modules/user';
  import { getFirstRouterNameByCurrentRoute, hasAnyPermission } from '@/utils/permission';

  import {
    AppRouteEnum,
    ClueRouteEnum,
    CustomerRouteEnum,
    DashboardRouteEnum,
    OpportunityRouteEnum,
    TenderRouteEnum,
    WorkbenchRouteEnum,
  } from '@/enums/routeEnum';

  import { MenuGroupOption, MenuOption } from 'naive-ui/es/menu/src/interface';

  const emit = defineEmits<{
    (e: 'openPersonalInfo', tab: PersonalEnum): void;
  }>();

  const { logout } = useUser();

  const { t } = useI18n();
  const appStore = useAppStore();
  const userStore = useUserStore();
  const licenseStore = useLicenseStore();
  const router = useRouter();
  const route = useRoute();
  const collapsed = ref(appStore.getMenuCollapsed);
  const menuValue = ref<string>(AppRouteEnum.SYSTEM_ORG);
  const expandedKeys = ref<string[]>([]);
  const personalMenuValue = ref<string>('');
  const personalTab = ref(PersonalEnum.INFO);
  const visitedKey = 'doNotShowPersonalExportAgain';
  const { addVisited, getIsVisited } = useVisit(visitedKey);

  watch(
    () => appStore.getMenuCollapsed,
    (value) => {
      collapsed.value = value;
    }
  );

  function renderIcon(type: string) {
    return () =>
      h(CrmIcon, {
        size: 16,
        type,
        class: 'text-[var(--text-n1)]',
      });
  }

  const hasExportPermission = computed(() =>
    hasAnyPermission(['CUSTOMER_MANAGEMENT:EXPORT', 'OPPORTUNITY_MANAGEMENT:EXPORT', 'CLUE_MANAGEMENT:EXPORT'])
  );

  const personalMenuOptions = computed(() => [
    {
      key: 'header',
      type: 'render',
      render: () =>
        h(
          NTooltip,
          {
            delay: 300,
          },
          {
            trigger: () =>
              h(
                'div',
                { class: 'personal-name one-line-text max-w-[110px]' },
                { default: () => userStore.userInfo.name }
              ),
            default: () => h('div', {}, { default: () => userStore.userInfo.name }),
          }
        ),
    },
    {
      key: 'header-divider',
      type: 'divider',
    },
    {
      label: t('module.personal.info'),
      key: AppRouteEnum.PERSONAL_INFO,
      icon: renderIcon('iconicon_set_up'),
    },
    {
      label: t('module.personal.plan'),
      key: AppRouteEnum.PERSONAL_PLAN,
      icon: renderIcon('iconicon_calendar1'),
    },
    ...(hasExportPermission.value
      ? [
          {
            label: t('module.personal.myExport'),
            key: AppRouteEnum.PERSONAL_EXPORT,
            icon: renderIcon('iconicon_export'),
          },
        ]
      : []),
    {
      label: t('module.logout'),
      key: AppRouteEnum.LOGOUT,
      icon: renderIcon('iconicon_logout'),
    },
  ]);

  function renderLabel(option: MenuOption | MenuGroupOption) {
    // 展开态菜单名已完整显示，悬浮重复弹文字纯干扰；折叠成图标时才用 tooltip 补全名称
    // 菜单名包一层 span，用于展开态文字超长省略；右侧指示灯由 CSS ::after 注入（仅一级菜单）
    if (!collapsed.value) {
      return h('span', { class: 'crm-menu-text' }, { default: () => option.label });
    }
    return h(
      NTooltip,
      {
        delay: 300,
      },
      {
        trigger: () => h('div', {}, { default: () => option.label }),
        default: () => h('div', {}, { default: () => option.label }),
      }
    );
  }

  const showPopModal = ref(false);
  function confirmHandler() {
    addVisited();
    showPopModal.value = false;
  }

  let timer: any = null;
  function initExportPop() {
    if (!getIsVisited() && hasExportPermission.value) {
      showPopModal.value = true;

      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
      timer = setTimeout(() => {
        confirmHandler();
        timer = null;
      }, 5000);
    }
  }

  function renderExpandMenuIcon() {
    // 单一向下 chevron：折叠态朝下、展开态朝上，旋转动画由 Naive 内建 transform(rotate 180deg) 承担
    return h(CrmIcon, {
      size: 14,
      type: 'iconicon_chevron_down',
    });
  }

  const isRequiredExportRoute = (key: OpportunityRouteEnum | ClueRouteEnum | CustomerRouteEnum) =>
    [AppRouteEnum.CLUE_MANAGEMENT, AppRouteEnum.OPPORTUNITY, AppRouteEnum.CUSTOMER].includes(
      key as OpportunityRouteEnum | ClueRouteEnum | CustomerRouteEnum
    );

  async function menuChange(key: string, item: MenuOption) {
    const routeItem = item as unknown as AppRouteRecordRaw;
    let name: string | symbol = key;
    if (routeItem.name === WorkbenchRouteEnum.WORKBENCH) {
      name = WorkbenchRouteEnum.WORKBENCH_BOARD;
    } else if (routeItem.meta?.hideChildrenInMenu) {
      name = getFirstRouterNameByCurrentRoute(routeItem.name as string) || key;
    }
    await router.push({ name });
    if (isRequiredExportRoute(key as OpportunityRouteEnum | ClueRouteEnum | CustomerRouteEnum)) {
      initExportPop();
    }
  }

  const personalMenuShow = ref(false);
  function personalMenuUpdateShow(value: boolean) {
    personalMenuShow.value = value;
  }

  const showPersonalExport = ref(false);

  async function personalMenuChange(key: string) {
    personalMenuValue.value = key;
    if (key === AppRouteEnum.PERSONAL_INFO || key === AppRouteEnum.PERSONAL_PLAN) {
      if (key === AppRouteEnum.PERSONAL_INFO) {
        personalTab.value = PersonalEnum.INFO;
      } else {
        personalTab.value = PersonalEnum.MY_PLAN;
      }
      emit('openPersonalInfo', personalTab.value);
    } else if (key === AppRouteEnum.PERSONAL_EXPORT) {
      showPersonalExport.value = true;
    } else {
      await userStore.logout();
      logout();
      if (!licenseStore.hasLicense()) {
        // license到期后，退出登录重置界面配置
        appStore.resetPageConfig();
        window.location.reload();
      }
    }
  }

  const { menuTree } = useMenuTree();

  function getMenuIcon(e: AppRouteRecordRaw) {
    if (appStore.getMenuIconStatus) {
      return e?.meta?.icon ? renderIcon(e.meta.icon) : null;
    }

    return collapsed.value && e?.meta?.collapsedLocale
      ? () => h('div', { class: `flex flex-nowrap text-[14px]` }, t(e?.meta?.collapsedLocale ?? ''))
      : null;
  }

  const menuOptions = computed<MenuOption[]>(() => {
    return mapTree(menuTree.value, (e: any) => {
      const menuChildren = mapTree(e.children);
      return e.meta.isTopMenu
        ? null
        : {
            ...e,
            label: t(e?.meta?.locale ?? ''),
            key: e.name,
            children: menuChildren.length ? menuChildren : undefined,
            icon: getMenuIcon(e),
          };
    }) as unknown as MenuOption[];
  });

  function setMenuValue(_route: RouteLocationNormalizedGeneric) {
    const hideChildrenRoute = [..._route.matched].reverse().find((item) => item.meta?.hideChildrenInMenu && item.name);

    if (_route.meta.isTopMenu) {
      menuValue.value = _route.matched[_route.matched.length - 2]
        ?.name as (typeof AppRouteEnum)[keyof typeof AppRouteEnum];
    } else if (hideChildrenRoute) {
      menuValue.value = hideChildrenRoute.name as (typeof AppRouteEnum)[keyof typeof AppRouteEnum];
    } else {
      menuValue.value = _route.name as (typeof AppRouteEnum)[keyof typeof AppRouteEnum];
      // 进入二级页面时保持所属一级菜单展开（含 system、收费管理等带子菜单的顶级菜单）
      // matched[0] 即一级菜单 record（实测 matched 只有两级，无根布局层）
      const topMenuName = _route.matched[0]?.name;
      if (topMenuName) {
        expandedKeys.value = [topMenuName as string];
      }
    }
  }

  onBeforeMount(() => {
    setMenuValue(router.currentRoute.value);

    const routeName = router.currentRoute.value.matched[0]?.name as
      | OpportunityRouteEnum
      | ClueRouteEnum
      | CustomerRouteEnum;
    if (isRequiredExportRoute(routeName)) {
      initExportPop();
    }
  });

  /**
   * 监听路由变化，切换菜单选中
   */
  listenerRouteChange((newRoute) => {
    setMenuValue(newRoute);
  }, true);

  watch(
    () => appStore.getRestoreMenuTimeStamp,
    (value) => {
      if (value) {
        setMenuValue(router.currentRoute.value);
      }
    }
  );

  watch(
    () => appStore.orgId,
    (orgId) => {
      if (orgId) {
        appStore.initModuleConfig();
        appStore.initNavTopConfig();
      }
    },
    { immediate: true }
  );
</script>

<style lang="less">
  .crm-layout-sider {
    --n-color: #e3e9f0;

    font-weight: 500;
    background-color: #e3e9f0;
    .n-scrollbar-content {
      @apply h-full;
    }
    .n-divider:not(.n-divider--vertical) {
      margin: 12px 0;
    }

    // ============ 菜单容器：左右留 14px，键帽不贴边（对齐设计稿 .menu padding） ============
    .n-menu {
      padding: 16px 14px 10px;
    }

    // ============ 一级菜单垂直间距：叶子/父项之间 10px，父标签与子项之间 10px ============
    .n-menu > .n-menu-item,
    .n-menu > .n-submenu {
      margin-top: 0;
      margin-bottom: 10px;
    }
    .n-menu > .n-submenu > .n-menu-item {
      margin-top: 0;
      margin-bottom: 10px;
    }

    // ============ 一级菜单键帽：渐变 + 阴影 + 圆角全落在同一可视层（::before 铺满） ============
    .n-menu > .n-menu-item > .n-menu-item-content,
    .n-menu > .n-submenu > .n-menu-item > .n-menu-item-content {
      padding-right: 12px !important;
      padding-left: 12px !important;
      height: 40px;
      transition: transform 0.18s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.18s ease;
      will-change: transform;
      &::before {
        right: 0;
        left: 0;
        border-radius: 11px;
        background-color: transparent;
        box-shadow: 4px 4px 10px rgb(140 158 175 / 75%), -4px -4px 10px #ffffff, inset 0 1px 0 #ffffff;
        transition: background-image 0.18s ease, box-shadow 0.18s ease;
        background-image: linear-gradient(145deg, #ffffff, #e7edf4);
      }
    }

    // 悬浮：轻微上浮 + 阴影增强（按压态不参与）
    .n-menu
      > .n-menu-item
      > .n-menu-item-content:not(.n-menu-item-content--selected):not(.n-menu-item-content--child-active):hover,
    .n-menu
      > .n-submenu
      > .n-menu-item
      > .n-menu-item-content:not(.n-menu-item-content--selected):not(.n-menu-item-content--child-active):hover {
      transform: translateY(-2px);
      &::before {
        box-shadow: 6px 6px 14px rgb(140 158 175 / 75%), -6px -6px 14px #ffffff, inset 0 1px 0 #ffffff;
      }
    }

    // 选中 / 含激活子项 / 已展开父项：内阴影按压态（下沉 + 覆盖悬浮）
    .n-menu > .n-menu-item > .n-menu-item-content--selected,
    .n-menu > .n-submenu > .n-menu-item > .n-menu-item-content--child-active,
    .n-menu > .n-submenu[aria-expanded='true'] > .n-menu-item > .n-menu-item-content {
      transform: translateY(2px);
      &::before {
        background-image: linear-gradient(145deg, #dfe6ee, #f2f7fb);
        box-shadow: inset 4px 4px 8px rgb(140 158 175 / 85%), inset -4px -4px 8px rgb(255 255 255 / 95%);
      }
    }

    // 一级菜单：文字 + 右侧指示灯（仅一级，二级菜单无指示灯、走左侧圆点）
    .n-menu > .n-menu-item > .n-menu-item-content > .n-menu-item-content-header,
    .n-menu > .n-submenu > .n-menu-item > .n-menu-item-content > .n-menu-item-content-header {
      display: flex;
      align-items: center;
      overflow: visible;
      &::after {
        content: '';
        margin-left: auto;
        flex-shrink: 0;
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: #c3cfdb;
        box-shadow: inset 1px 1px 2px rgb(120 140 160 / 95%), inset -1px -1px 1.5px rgb(255 255 255 / 90%);
        transition: background 0.3s ease, box-shadow 0.3s ease;
      }
    }

    // 一级菜单指示灯：选中时点亮（白色呼吸灯）
    .n-menu > .n-menu-item > .n-menu-item-content--selected > .n-menu-item-content-header::after,
    .n-menu > .n-submenu > .n-menu-item > .n-menu-item-content--selected > .n-menu-item-content-header::after {
      background: #ffffff;
      box-shadow: 0 0 5px #ffffff, 0 0 11px rgb(255 255 255 / 95%), 0 0 18px rgb(255 255 255 / 60%),
        inset 0 0 3px #ffffff;
      animation: crm-lamp-pulse 2.4s ease-in-out infinite;
    }

    // 展开箭头：默认灰、悬浮深灰、展开主题色（旋转动画由 Naive 内建 transform 承担）
    .n-menu > .n-submenu > .n-menu-item > .n-menu-item-content > .n-menu-item-content__arrow {
      color: #9dacb9;
      transition: color 0.22s ease, transform 0.28s cubic-bezier(0.2, 0.8, 0.2, 1);
    }
    .n-menu > .n-submenu > .n-menu-item > .n-menu-item-content:hover > .n-menu-item-content__arrow {
      color: #6b7b8b;
    }
    .n-menu > .n-submenu[aria-expanded='true'] > .n-menu-item > .n-menu-item-content > .n-menu-item-content__arrow {
      color: var(--primary-0);
    }

    // ============ 二级菜单：左侧导轨线 + 圆点项 ============
    .n-menu > .n-submenu > .n-submenu-children {
      position: relative;
      padding: 6px 0;
      &::before {
        position: absolute;
        top: 12px;
        bottom: 12px;
        left: 19px;
        width: 1.5px;
        border-radius: 2px;
        background: linear-gradient(
          180deg,
          transparent 0%,
          rgb(140 158 175 / 35%) 12%,
          rgb(140 158 175 / 35%) 88%,
          transparent 100%
        );
        content: '';
        pointer-events: none;
      }
    }

    // 二级项之间 2px 间距（首项不额外留白）
    .n-menu > .n-submenu > .n-submenu-children > .n-menu-item {
      margin-top: 0;
      margin-bottom: 0;
    }
    .n-menu > .n-submenu > .n-submenu-children > .n-menu-item + .n-menu-item {
      margin-top: 2px;
    }
    .n-menu > .n-submenu > .n-submenu-children > .n-menu-item > .n-menu-item-content {
      padding-right: 12px !important;
      padding-left: 42px !important;
      height: 36px;
      border-radius: 9px;
      color: #4d5c6b;
      transition: color 0.2s ease, background-color 0.2s ease, background-image 0.2s ease, box-shadow 0.2s ease;
      &::before {
        position: absolute;
        top: 50%;
        right: auto;
        bottom: auto;
        left: 26px;
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: #b8c4d0;
        box-shadow: inset 1px 1px 1.5px rgb(120 140 160 / 85%), inset -1px -1px 1px rgb(255 255 255 / 85%);
        transition: transform 0.24s cubic-bezier(0.2, 0.8, 0.2, 1), background 0.22s ease, box-shadow 0.22s ease;
        content: '';
        transform: translateY(-50%) scale(0.55);
      }
      &:hover {
        color: #1f2d3d;
        background-color: rgb(255 255 255 / 55%);
        &::before {
          transform: translateY(-50%) scale(0.8);
          background: #8fa0b1;
        }
      }
    }
    .n-menu > .n-submenu > .n-submenu-children > .n-menu-item > .n-menu-item-content--selected {
      font-weight: 600;
      color: #0a9c8e;
      box-shadow: 3px 3px 8px rgb(140 158 175 / 50%), -3px -3px 8px rgb(255 255 255 / 95%), inset 0 1px 0 #ffffff;
      background-image: linear-gradient(145deg, #ffffff, #eaf6f4);
      &::before {
        transform: translateY(-50%) scale(1);
        background: var(--primary-8);
        box-shadow: 0 0 6px rgb(15 184 168 / 85%), 0 0 12px rgb(15 184 168 / 50%), inset 0 0 2px #ffffff;
      }
    }

    // 图标默认色（选中态由下方全局规则覆盖为主题色）
    .n-menu-item-content__icon .n-icon {
      color: #3d4d5c;
    }
  }

  @keyframes crm-lamp-pulse {
    0%,
    100% {
      box-shadow: 0 0 5px #ffffff, 0 0 11px rgb(255 255 255 / 95%), 0 0 18px rgb(255 255 255 / 60%),
        inset 0 0 3px #ffffff;
    }
    50% {
      box-shadow: 0 0 7px #ffffff, 0 0 16px rgb(255 255 255 / 100%), 0 0 26px rgb(255 255 255 / 80%),
        inset 0 0 4px #ffffff;
    }
  }
  .crm-menu-text {
    display: block;
    overflow: hidden;
    min-width: 0;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .n-menu-item-content--selected,
  .n-menu-item-content--child-active {
    .n-icon {
      color: var(--n-item-text-color-active) !important;
    }
  }
  .personal-menu {
    min-width: 120px;
    .n-menu .n-menu-item {
      align-items: flex-start;
      margin-top: 0;
      padding: 4px 12px;
      height: 30px;
      border-radius: 4px;
    }
    .n-menu .n-menu-item:hover {
      transition: 0.7s;

      --n-item-color-hover: var(--primary-7);
    }
    .n-menu-item-content {
      padding-left: 0 !important;
      .n-menu-item-content-header {
        color: var(--text-n2);
      }
      .n-menu-item-content__icon {
        width: 16px;
        height: 16px;
        color: var(--text-n2);
      }
    }
    .n-menu-item-content::before {
      top: -4px;
      right: -8px;
      bottom: -4px;
      left: -8px;
    }
  }
  .personal-popover {
    min-width: 120px;
    background-color: var(--text-n10);
    .n-popover__content {
      padding: 0 !important;
    }
  }
  .personal-name {
    padding: 4px 8px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-n1);
    line-height: 22px;
  }
  .personal-dropdown {
    .n-dropdown-option-body {
      color: var(--text-n2) !important;
    }
  }
</style>

<style lang="less" scoped>
  .personal-export-pop {
    position: absolute;
    right: 0;
    bottom: 0;
  }
</style>
