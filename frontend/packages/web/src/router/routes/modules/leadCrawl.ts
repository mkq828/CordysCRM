import { LeadCrawlRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 线索爬取 + 竞品洞察（功能 10，企业版）。菜单对所有登录用户可见，
// 版本门控由后端 hasFeature(lead_crawl) 控制（与 AI 方案/PPT、话术库一致）。
const leadCrawl: AppRouteRecordRaw = {
  path: '/ai-lead-crawl',
  name: LeadCrawlRouteEnum.LEAD_CRAWL,
  redirect: '/ai-lead-crawl/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.aiLeadCrawl',
    permissions: [],
    icon: 'iconicon_crmbot',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.aiLeadCrawl',
  },
  children: [
    {
      path: 'index',
      name: LeadCrawlRouteEnum.LEAD_CRAWL_INDEX,
      component: () => import('@/views/leadCrawl/index.vue'),
      meta: {
        locale: 'menu.aiLeadCrawl',
        permissions: [],
      },
    },
  ],
};

export default leadCrawl;
