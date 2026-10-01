import common from './common';
import localeSettings from './settings';
import sys from './sys';
import dayjsLocale from 'dayjs/locale/en';

const _Cmodules: any = import.meta.glob('../../components/**/locale/en-US.ts', { eager: true });
const _Vmodules: any = import.meta.glob('../../views/**/locale/en-US.ts', { eager: true });
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
    'menu.workbench': 'Home',
    'menu.workbench.smart': 'Smart Workspace',
    'menu.workbench.board': 'My Dashboard',
    'menu.settings': 'Settings',
    'menu.collapsedSettings': 'System',
    'menu.settings.org': 'Organization',
    'menu.settings.permission': 'Roles',
    'menu.settings.moduleSetting': 'Module',
    'menu.opportunity': 'Opportunity',
    'menu.quotation': 'Quotation',
    'menu.collapsedOpportunity': 'Opportunity',
    'menu.collapsedProduct': 'Product',
    'menu.clue': 'Lead',
    'menu.customer': 'Account',
    'menu.contact': 'Contact',
    'menu.dashboard': 'Dashboard',
    'menu.platformDashboard': 'Platform Overview',
    'menu.agent': 'Agent',
    'menu.custom_form': 'Custom Form',
    'menu.tender': 'Tender',
    'menu.finance': 'Finance',
    'menu.platformFinance': 'Platform Billing',
    'menu.platformFinanceContract': 'Contracts',
    'menu.platformFinancePaymentRecord': 'Payments',
    'menu.platformFinanceInvoice': 'Invoices',
    'menu.platformFinanceRevenue': 'Revenue',
    'menu.cityManager': 'City Partners',
    'menu.cityManagerAccount': 'Partner Accounts',
    'menu.cityManagerDashboard': 'Performance',
    'menu.aiQuota': 'My AI Quota',
    'menu.platformAiQuota': 'AI Quota Mgmt',
    'menu.customForm': 'Custom Form',
    'menu.suggestion': 'Suggestions',
    'menu.settings.businessSetting': 'Enterprise',
    'menu.settings.license': 'License',
    'menu.settings.messageSetting': 'Notification',
    'menu.settings.processSetting': 'Process',
    'menu.settings.approvalFlow': 'Approval Flow',
    'menu.settings.workflowSetting': 'Workflow',
    'menu.platformAdmin': 'Platform Admin',
    'menu.platformAdmin.registerAudit': 'Register Audit',
    'menu.platformAdmin.paidUser': 'Paid Users',
    'menu.platformAdmin.edition': 'Editions',
    'menu.platformAdmin.log': 'Logs',
    'menu.platformAdmin.bugReport': 'Bug Report',
    'navbar.action.locale': 'Switch to English',
    ...sys,
    ...localeSettings,
    ...result,
    ...common,
  },
  dayjsLocale,
  dayjsLocaleName: 'en-US',
};
