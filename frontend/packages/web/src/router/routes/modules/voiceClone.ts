import { VoiceCloneRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 声音克隆（功能 11，企业版）。菜单对所有登录用户可见，
// 版本门控由后端 hasFeature(ai_voice_clone) 控制（与短视频、问数 BI 一致）。
const voiceClone: AppRouteRecordRaw = {
  path: '/ai-voice-clone',
  name: VoiceCloneRouteEnum.VOICE_CLONE,
  redirect: '/ai-voice-clone/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.voiceClone',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.voiceClone',
  },
  children: [
    {
      path: 'index',
      name: VoiceCloneRouteEnum.VOICE_CLONE_INDEX,
      component: () => import('@/views/voiceClone/index.vue'),
      meta: {
        locale: 'menu.voiceClone',
        permissions: [],
      },
    },
  ],
};

export default voiceClone;
