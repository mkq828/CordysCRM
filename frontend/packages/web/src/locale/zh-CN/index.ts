import common from './common';
import localeSettings from './settings';
import sys from './sys';
import dayjsLocale from 'dayjs/locale/zh-cn';

const _Cmodules: any = import.meta.glob('../../components/**/locale/zh-CN.ts', { eager: true });
const _Vmodules: any = import.meta.glob('../../views/**/locale/zh-CN.ts', { eager: true });
let result = {};
Object.keys(_Cmodules).forEach((key) => {
  const defaultModule = _Cmodules[key as any].default;
  if (!defaultModule) return;
  result = { ...result, ...defaultModule };
});
Object.keys(_Vmodules).forEach((key) => {
  const defaultModule = _Vmodules[key as any].default;
  if (!defaultModule) return;
  result = { ...result, ...defaultModule };
});
export default {
  message: {
    'menu.workbench': '首页',
    'menu.workbench.smart': '智慧工作台',
    'menu.workbench.board': '我的看板',
    'menu.workbench.callReview': '智能通话复盘',
    'menu.workbench.dmProfile': '私信获客画像',
    'menu.workbench.dmProfileGuide': '使用说明',
    'menu.settings': '系统',
    'menu.collapsedSettings': '系统',
    'menu.settings.org': '组织架构',
    'menu.settings.permission': '角色权限',
    'menu.settings.moduleSetting': '模块配置',
    'menu.opportunity': '商机',
    'menu.quotation': '报价',
    'menu.collapsedOpportunity': '商机',
    'menu.collapsedProduct': '产品',
    'menu.clue': '线索',
    'menu.customer': '客户',
    'menu.contact': '联系人',
    'menu.dashboard': '仪表板',
    'menu.businessScreen': 'AI 经营决策中枢',
    'menu.platformDashboard': '平台大屏',
    'menu.agent': '智能体',
    'menu.custom_form': '自定义表单',
    'menu.tender': '标讯',
    'menu.finance': '财务',
    'menu.platformFinance': '租户全生命周期',
    'menu.platformFinanceContract': '合同',
    'menu.platformFinancePaymentRecord': '回款',
    'menu.platformFinanceInvoice': '发票',
    'menu.platformFinanceRevenue': '营收看板',
    'menu.platformManual': '管理员手册',
    'menu.platformManualOnboardingFlow': '租户开通流程',
    'menu.platformManualNoticeRules': '通知规则',
    'menu.platformManualVerifyRules': '核销与授权',
    'menu.platformManualRolePermission': '角色与权限',
    'menu.platformManualAiFeatures': 'AI 功能放行',
    'menu.platformManualConfigItems': '配置项说明',
    'menu.cityManager': '城市合伙人',
    'menu.cityManagerAccount': '合伙人账号',
    'menu.cityManagerDashboard': '业绩看板',
    'menu.aiQuota': '我的 AI 额度',
    'menu.salesScript': '销售话术库',
    'menu.aiContent': 'AI 获客内容',
    'menu.aiKnowledge': '企业知识库',
    'menu.aiPpt': '方案/PPT',
    'menu.aiLeadCrawl': '竞品洞察',
    'menu.aiLeadMining': '潜客挖掘',
    'menu.aiTenderSubscription': '标讯订阅',
    'menu.aiCompetitorMonitor': '竞品动态',
    'menu.aiSentimentMonitor': '舆情监控',
    'menu.aiBossBrief': '老板晨报',
    'menu.aiVideo': '短视频生成',
    'menu.aiReplica': '爆款复刻',
    'menu.aiDrama': 'AI 漫剧',
    'menu.pending': '待完善功能',
    'menu.aiCreation': 'AI 创作中心',
    'menu.aiCreation.tool': '工具中心',
    'menu.askBi': '问数 BI',
    'menu.voiceClone': '声音克隆',
    'menu.aiEmployee': 'AI 员工',
    'menu.effects': '视觉特效库',
    'menu.platformAiQuota': 'AI 额度管理',
    'menu.settings.businessSetting': '企业设置',
    'menu.settings.license': 'License',
    'menu.settings.messageSetting': '消息设置',
    'menu.settings.processSetting': '流程设置',
    'menu.settings.approvalFlow': '审批流',
    'menu.settings.workflowSetting': '工作流',
    'menu.customForm': '自定义表单',
    'menu.suggestion': '需求建议',
    'menu.platformAdmin': '平台运营',
    'menu.platformAdmin.registerAudit': '注册审核',
    'menu.platformAdmin.paidUser': '付费用户',
    'menu.platformAdmin.edition': '版本套餐',
    'menu.platformAdmin.planApplication': '续费申请',
    'menu.platformAdmin.log': '系统日志',
    'menu.platformAdmin.bugReport': '问题反馈',
    'menu.platformAdmin.securityEvent': '安全事件',
    'menu.platformAdmin.jobExecLog': '定时任务执行记录',
    'navbar.action.locale': '切换为中文',
    ...sys,
    ...localeSettings,
    ...result,
    ...common,
  },
  dayjsLocale,
  dayjsLocaleName: 'zh-CN',
};
