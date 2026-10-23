import {
  AgentRouteEnum,
  AiCreationRouteEnum,
  AiEmployeeRouteEnum,
  AskBiRouteEnum,
  BossBriefRouteEnum,
  CompetitorMonitorRouteEnum,
  ContentRouteEnum,
  KnowledgeRouteEnum,
  LeadCrawlRouteEnum,
  LeadMiningRouteEnum,
  PptRouteEnum,
  ScriptRouteEnum,
  SentimentMonitorRouteEnum,
  TenderSubscriptionRouteEnum,
  VoiceCloneRouteEnum,
  WorkbenchRouteEnum,
} from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

// AI 创作中心：统一入口，收编所有「已上线」的 AI 赋能模块。
// 菜单对所有登录用户可见，各子项权限照搬原独立路由；版本门控由后端 hasFeature 控制。
// 子路由用绝对路径保持原 URL（如 /agent/index、/workbench/smart）不变，只改菜单归属。
const aiCreation: AppRouteRecordRaw = {
  path: '/ai-creation',
  name: AiCreationRouteEnum.AI_CREATION,
  redirect: '/ai-creation/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.aiCreation',
    permissions: [],
    icon: 'iconicon_crmbot',
    collapsedLocale: 'menu.aiCreation',
  },
  children: [
    {
      path: '/ai-creation/index',
      name: AiCreationRouteEnum.AI_CREATION_INDEX,
      component: () => import('@/views/aiCreation/index.vue'),
      meta: {
        locale: 'menu.aiCreation.tool',
        permissions: [],
      },
    },
    {
      path: '/workbench/smart',
      name: WorkbenchRouteEnum.AI_SMART,
      component: () => import('@/views/workbench/smart/index.vue'),
      meta: {
        locale: 'menu.workbench.smart',
        permissions: [],
      },
    },
    {
      path: '/workbench/callReview',
      name: WorkbenchRouteEnum.AI_CALL_REVIEW,
      component: () => import('@/views/workbench/callReview/index.vue'),
      meta: {
        locale: 'menu.workbench.callReview',
        permissions: [],
      },
    },
    {
      path: '/workbench/dmProfile',
      name: WorkbenchRouteEnum.AI_DM_PROFILE,
      component: () => import('@/views/workbench/dmProfile/index.vue'),
      meta: {
        locale: 'menu.workbench.dmProfile',
        permissions: [],
      },
    },
    {
      path: '/agent/index',
      name: AgentRouteEnum.AGENT_INDEX,
      component: () => import('@/views/agent/index.vue'),
      meta: {
        locale: 'menu.agent',
        permissions: ['AGENT:READ'],
      },
    },
    {
      path: '/ai-employee/index',
      name: AiEmployeeRouteEnum.AI_EMPLOYEE_INDEX,
      component: () => import('@/views/aiEmployee/index.vue'),
      meta: {
        locale: 'menu.aiEmployee',
        permissions: [],
      },
    },
    {
      path: '/sales-script/index',
      name: ScriptRouteEnum.SCRIPT_INDEX,
      component: () => import('@/views/script/index.vue'),
      meta: {
        locale: 'menu.salesScript',
        permissions: [],
      },
    },
    {
      path: '/ai-content/index',
      name: ContentRouteEnum.CONTENT_INDEX,
      component: () => import('@/views/content/index.vue'),
      meta: {
        locale: 'menu.aiContent',
        permissions: [],
      },
    },
    {
      path: '/ai-knowledge/index',
      name: KnowledgeRouteEnum.KNOWLEDGE_INDEX,
      component: () => import('@/views/knowledge/index.vue'),
      meta: {
        locale: 'menu.aiKnowledge',
        permissions: [],
      },
    },
    {
      path: '/ai-lead-crawl/index',
      name: LeadCrawlRouteEnum.LEAD_CRAWL_INDEX,
      component: () => import('@/views/leadCrawl/index.vue'),
      meta: {
        locale: 'menu.aiLeadCrawl',
        permissions: [],
      },
    },
    {
      path: '/ai-lead-mining/index',
      name: LeadMiningRouteEnum.LEAD_MINING_INDEX,
      component: () => import('@/views/leadMining/index.vue'),
      meta: {
        locale: 'menu.aiLeadMining',
        permissions: [],
      },
    },
    {
      path: '/ai-tender-subscription/index',
      name: TenderSubscriptionRouteEnum.TENDER_SUBSCRIPTION_INDEX,
      component: () => import('@/views/tenderSubscription/index.vue'),
      meta: {
        locale: 'menu.aiTenderSubscription',
        permissions: [],
        // 「AI 招标订阅」随大单一起停用，菜单隐藏（可恢复）
        hideInMenu: true,
      },
    },
    {
      path: '/ai-competitor-monitor/index',
      name: CompetitorMonitorRouteEnum.COMPETITOR_MONITOR_INDEX,
      component: () => import('@/views/competitorMonitor/index.vue'),
      meta: {
        locale: 'menu.aiCompetitorMonitor',
        permissions: [],
      },
    },
    {
      path: '/ai-sentiment-monitor/index',
      name: SentimentMonitorRouteEnum.SENTIMENT_MONITOR_INDEX,
      component: () => import('@/views/sentimentMonitor/index.vue'),
      meta: {
        locale: 'menu.aiSentimentMonitor',
        permissions: [],
      },
    },
    {
      path: '/ai-boss-brief/index',
      name: BossBriefRouteEnum.BOSS_BRIEF_INDEX,
      component: () => import('@/views/bossBrief/index.vue'),
      meta: {
        locale: 'menu.aiBossBrief',
        permissions: [],
      },
    },
    {
      path: '/ai-bi/index',
      name: AskBiRouteEnum.ASK_BI_INDEX,
      component: () => import('@/views/askBi/index.vue'),
      meta: {
        locale: 'menu.askBi',
        permissions: [],
      },
    },
    {
      path: '/ai-voice-clone/index',
      name: VoiceCloneRouteEnum.VOICE_CLONE_INDEX,
      component: () => import('@/views/voiceClone/index.vue'),
      meta: {
        locale: 'menu.voiceClone',
        permissions: [],
      },
    },
    {
      path: '/ai-ppt/index',
      name: PptRouteEnum.PPT_INDEX,
      component: () => import('@/views/ppt/index.vue'),
      meta: {
        locale: 'menu.aiPpt',
        permissions: [],
      },
    },
  ],
};

export default aiCreation;
