import { AiCreationRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// AI 创作中心（一级入口：工具中心 / 模板库 / 资产库 / 我的作品）。
// 菜单对所有登录用户可见，版本门控由后端 hasFeature(ai_tool) 控制（与短视频、线索爬取一致）。
const aiCreation: AppRouteRecordRaw = {
  path: '/ai-creation',
  name: AiCreationRouteEnum.AI_CREATION,
  redirect: '/ai-creation/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.aiCreation',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.aiCreation',
  },
  children: [
    {
      path: 'index',
      name: AiCreationRouteEnum.AI_CREATION_INDEX,
      component: () => import('@/views/aiCreation/index.vue'),
      meta: {
        locale: 'menu.aiCreation',
        permissions: [],
      },
    },
  ],
};

export default aiCreation;
