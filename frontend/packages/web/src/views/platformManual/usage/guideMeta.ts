import aiQuota from '@/assets/platform-manual/ai-quota.png';
import bugReport from '@/assets/platform-manual/bug-report.png';
import cityAccount from '@/assets/platform-manual/city-account.png';
import cityDashboard from '@/assets/platform-manual/city-dashboard.png';
import contract from '@/assets/platform-manual/contract.png';
import edition from '@/assets/platform-manual/edition.png';
import errorLog from '@/assets/platform-manual/error-log.png';
import invoice from '@/assets/platform-manual/invoice.png';
import jobExecLog from '@/assets/platform-manual/job-exec-log.png';
import log from '@/assets/platform-manual/log.png';
import paidUser from '@/assets/platform-manual/paid-user.png';
import paymentRecord from '@/assets/platform-manual/payment-record.png';
import planApplication from '@/assets/platform-manual/plan-application.png';
import registerAudit from '@/assets/platform-manual/register-audit.png';
import revenue from '@/assets/platform-manual/revenue.png';
import securityEvent from '@/assets/platform-manual/security-event.png';

export interface GuideMeta {
  key: string;
  label: string;
  icon: string;
  color: string;
  screenshot: string;
}

// 使用说明：每个二级菜单对应一个 guide，正文文案在 views/platformManual/usage/locale/*.ts。
// 截图通过 Vite 静态资源导入，跟代码走、离线可看。
export const guideMetas: GuideMeta[] = [
  {
    key: 'registerAudit',
    label: 'menu.platformAdmin.registerAudit',
    icon: 'iconicon_books',
    color: '#4282FF',
    screenshot: registerAudit,
  },
  {
    key: 'paidUser',
    label: 'menu.platformAdmin.paidUser',
    icon: 'iconicon_usergroup',
    color: '#4282FF',
    screenshot: paidUser,
  },
  {
    key: 'planApplication',
    label: 'menu.platformAdmin.planApplication',
    icon: 'iconicon_refresh',
    color: '#4282FF',
    screenshot: planApplication,
  },
  {
    key: 'contract',
    label: 'menu.platformFinanceContract',
    icon: 'iconicon_money_circle',
    color: '#4282FF',
    screenshot: contract,
  },
  {
    key: 'paymentRecord',
    label: 'menu.platformFinancePaymentRecord',
    icon: 'iconicon_creditcard',
    color: '#4282FF',
    screenshot: paymentRecord,
  },
  {
    key: 'invoice',
    label: 'menu.platformFinanceInvoice',
    icon: 'iconicon_file',
    color: '#4282FF',
    screenshot: invoice,
  },
  {
    key: 'revenue',
    label: 'menu.platformFinanceRevenue',
    icon: 'iconicon_aiming',
    color: '#4282FF',
    screenshot: revenue,
  },
  {
    key: 'cityAccount',
    label: 'menu.cityManagerAccount',
    icon: 'iconicon_usergroup',
    color: '#8b5cf6',
    screenshot: cityAccount,
  },
  {
    key: 'cityDashboard',
    label: 'menu.cityManagerDashboard',
    icon: 'iconicon_aiming',
    color: '#8b5cf6',
    screenshot: cityDashboard,
  },
  {
    key: 'edition',
    label: 'menu.platformAdmin.edition',
    icon: 'iconicon_setting',
    color: '#f59e0b',
    screenshot: edition,
  },
  { key: 'log', label: 'menu.platformAdmin.log', icon: 'iconicon_file', color: '#f59e0b', screenshot: log },
  {
    key: 'bugReport',
    label: 'menu.platformAdmin.bugReport',
    icon: 'iconicon_notification',
    color: '#f59e0b',
    screenshot: bugReport,
  },
  {
    key: 'securityEvent',
    label: 'menu.platformAdmin.securityEvent',
    icon: 'iconicon_notification',
    color: '#f59e0b',
    screenshot: securityEvent,
  },
  {
    key: 'jobExecLog',
    label: 'menu.platformAdmin.jobExecLog',
    icon: 'iconicon_refresh',
    color: '#f59e0b',
    screenshot: jobExecLog,
  },
  {
    key: 'errorLog',
    label: 'menu.platformAdmin.errorLog',
    icon: 'iconicon_file',
    color: '#f59e0b',
    screenshot: errorLog,
  },
  {
    key: 'aiQuota',
    label: 'platformManualUsage.aiQuota.label',
    icon: 'iconicon_crmbot',
    color: '#10b981',
    screenshot: aiQuota,
  },
];
