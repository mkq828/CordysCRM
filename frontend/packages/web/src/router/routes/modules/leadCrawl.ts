import { LeadCrawlRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// 线索爬取 + 竞品洞察（功能 10）暂未完成（未接入真实企查查 Key 验证），
// 现仅 admin 可见、不对外开放。permissions 用专属 code（未写入后端权限种子），
// 普通角色永远无法被授权看到，admin 通过 isAdmin 短路放行（仿 platformDashboard）。
const leadCrawl: AppRouteRecordRaw = {
  path: '/ai-lead-crawl',
  name: LeadCrawlRouteEnum.LEAD_CRAWL,
  redirect: '/ai-lead-crawl/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.aiLeadCrawl',
    permissions: ['ADMIN_LEAD_CRAWL:READ'],
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
        permissions: ['ADMIN_LEAD_CRAWL:READ'],
      },
    },
  ],
};

export default leadCrawl;
