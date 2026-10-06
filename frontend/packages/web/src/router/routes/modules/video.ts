import { VideoRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 短视频生成（功能 7，专业版+）。菜单对所有登录用户可见，
// 版本门控由后端 hasFeature(ai_video) 控制（与线索爬取、私信画像一致）。
const video: AppRouteRecordRaw = {
  path: '/ai-video',
  name: VideoRouteEnum.VIDEO,
  redirect: '/ai-video/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.aiVideo',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.aiVideo',
  },
  children: [
    {
      path: 'index',
      name: VideoRouteEnum.VIDEO_INDEX,
      component: () => import('@/views/video/index.vue'),
      meta: {
        locale: 'menu.aiVideo',
        permissions: [],
      },
    },
  ],
};

export default video;
