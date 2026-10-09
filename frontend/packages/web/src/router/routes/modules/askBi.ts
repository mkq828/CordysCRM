import { AskBiRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// AI 问数 BI（功能 12，企业版）。菜单对所有登录用户可见，
// 版本门控由后端 hasFeature(ai_ask_bi) 控制（与短视频、线索爬取一致）。
const askBi: AppRouteRecordRaw = {
  path: '/ai-bi',
  name: AskBiRouteEnum.ASK_BI,
  redirect: '/ai-bi/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.askBi',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.askBi',
  },
  children: [
    {
      path: 'index',
      name: AskBiRouteEnum.ASK_BI_INDEX,
      component: () => import('@/views/askBi/index.vue'),
      meta: {
        locale: 'menu.askBi',
        permissions: [],
      },
    },
  ],
};

export default askBi;
