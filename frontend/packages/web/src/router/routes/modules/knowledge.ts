import { KnowledgeRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const knowledge: AppRouteRecordRaw = {
  path: '/ai-knowledge',
  name: KnowledgeRouteEnum.KNOWLEDGE,
  redirect: '/ai-knowledge/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.aiKnowledge',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.aiKnowledge',
  },
  children: [
    {
      path: 'index',
      name: KnowledgeRouteEnum.KNOWLEDGE_INDEX,
      component: () => import('@/views/knowledge/index.vue'),
      meta: {
        locale: 'menu.aiKnowledge',
        permissions: [],
      },
    },
  ],
};

export default knowledge;
