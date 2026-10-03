export enum SystemRouteEnum {
  SYSTEM = 'system',
  SYSTEM_ORG = 'systemOrg',
  SYSTEM_ROLE = 'systemRole',
  SYSTEM_MODULE = 'systemModule',
  SYSTEM_BUSINESS = 'systemBusiness',
  SYSTEM_LICENSE = 'systemLicense',
  SYSTEM_LOG = 'systemLog',
  SYSTEM_MESSAGE = 'systemMessage',
  SYSTEM_PROCESS = 'systemProcess',
  SYSTEM_PROCESS_INDEX = 'systemProcessIndex',
  SYSTEM_PROCESS_WORKFLOW = 'systemProcessWorkflow',
  SYSTEM_REGISTER_AUDIT = 'systemRegisterAudit',
  SYSTEM_BUG_REPORT = 'systemBugReport',
  SYSTEM_PAID_USER = 'systemPaidUser',
  SYSTEM_EDITION = 'systemEdition',
  SYSTEM_PLAN_APPLICATION = 'systemPlanApplication',
}

export enum OpportunityRouteEnum {
  OPPORTUNITY = 'opportunity',
  OPPORTUNITY_OPT = 'opportunityOpt',
  OPPORTUNITY_QUOTATION = 'opportunityQuotation',
}

export enum ClueRouteEnum {
  CLUE_MANAGEMENT = 'leadManagement',
  CLUE_MANAGEMENT_CLUE = 'leadManagementLead',
  CLUE_MANAGEMENT_POOL = 'leadManagementPool',
}

export enum CustomerRouteEnum {
  CUSTOMER = 'account',
  CUSTOMER_INDEX = 'accountIndex',
  CUSTOMER_CONTACT = 'accountContact',
  CUSTOMER_OPEN_SEA = 'accountOpenSea',
}

export enum ContractRouteEnum {
  CONTRACT = 'contract',
  CONTRACT_INDEX = 'contractIndex',
  CONTRACT_PAYMENT = 'contractPaymentPlan',
  CONTRACT_PAYMENT_RECORD = 'contractPaymentRecord',
  CONTRACT_BUSINESS_NAME = 'contractBusinessName',
  CONTRACT_BANK_ACCOUNT = 'contractBankAccount',
  CONTRACT_INVOICE = 'contractInvoice',
}

export enum OrderRouteEnum {
  ORDER = 'order',
  ORDER_INDEX = 'orderIndex',
}

export enum ProductRouteEnum {
  PRODUCT = 'product',
  PRODUCT_PRO = 'productPro',
  PRODUCT_PRICE = 'productPrice',
}

export enum PersonalRouteEnum {
  PERSONAL_INFO = 'personalInfo',
  PERSONAL_PLAN = 'personalPlan',
  PERSONAL_EXPORT = 'personalExport',
  LOGOUT = 'logout',
}

export enum WorkbenchRouteEnum {
  WORKBENCH = 'workbench',
  WORKBENCH_SMART = 'workbenchSmart',
  WORKBENCH_BOARD = 'workbenchBoard',
}

export enum AgentRouteEnum {
  AGENT = 'agent',
  AGENT_INDEX = 'agentIndex',
}

export enum DashboardRouteEnum {
  DASHBOARD = 'dashboard',
  DASHBOARD_INDEX = 'dashboardIndex',
  DASHBOARD_LINK = 'dashboardLink',
  DASHBOARD_MODULE = 'dashboardModule',
  PLATFORM_DASHBOARD = 'platformDashboard',
  PLATFORM_DASHBOARD_INDEX = 'platformDashboardIndex',
}

export enum TenderRouteEnum {
  TENDER = 'tender',
  TENDER_INDEX = 'tenderIndex',
}

export enum FinanceRouteEnum {
  FINANCE = 'finance',
  FINANCE_INDEX = 'financeIndex',
}

export enum PlatformFinanceRouteEnum {
  PLATFORM_FINANCE = 'platformFinance',
  PLATFORM_FINANCE_CONTRACT = 'platformFinanceContract',
  PLATFORM_FINANCE_PAYMENT_RECORD = 'platformFinancePaymentRecord',
  PLATFORM_FINANCE_INVOICE = 'platformFinanceInvoice',
  PLATFORM_FINANCE_REVENUE = 'platformFinanceRevenue',
}

export enum CityManagerRouteEnum {
  CITY_MANAGER = 'cityManager',
  CITY_MANAGER_ACCOUNT = 'cityManagerAccount',
  CITY_MANAGER_DASHBOARD = 'cityManagerDashboard',
}

export enum FullPageEnum {
  FULL_PAGE = 'fullPage',
  FULL_PAGE_DASHBOARD = 'fullPageDashboard',
  FULL_PAGE_EXPORT_QUOTATION = 'fullPageExportQuotation',
  FULL_PAGE_EXPORT_ORDER = 'fullPageExportOrder',
}

export enum CustomFormRouteEnum {
  CUSTOM_FORM = 'customForm',
  CUSTOM_FORM_INDEX = 'customFormIndex',
}

export enum SuggestionRouteEnum {
  SUGGESTION = 'suggestion',
  SUGGESTION_INDEX = 'suggestionIndex',
  SUGGESTION_DETAIL = 'suggestionDetail',
}

export enum AiQuotaRouteEnum {
  AI_QUOTA = 'aiQuota',
  AI_QUOTA_INDEX = 'aiQuotaIndex',
}

export enum ScriptRouteEnum {
  SCRIPT = 'salesScript',
  SCRIPT_INDEX = 'salesScriptIndex',
}

export enum ContentRouteEnum {
  CONTENT = 'aiContent',
  CONTENT_INDEX = 'aiContentIndex',
}

export enum PlatformAiQuotaRouteEnum {
  PLATFORM_AI_QUOTA = 'platformAiQuota',
  PLATFORM_AI_QUOTA_INDEX = 'platformAiQuotaIndex',
}

export enum PlatformAdminRouteEnum {
  PLATFORM_ADMIN = 'platformAdmin',
}

export const AppRouteEnum = {
  ...SystemRouteEnum,
  ...OpportunityRouteEnum,
  ...ClueRouteEnum,
  ...CustomerRouteEnum,
  ...ProductRouteEnum,
  ...PersonalRouteEnum,
  ...WorkbenchRouteEnum,
  ...DashboardRouteEnum,
  ...AgentRouteEnum,
  ...ContractRouteEnum,
  ...OrderRouteEnum,
  ...TenderRouteEnum,
  ...FinanceRouteEnum,
  ...PlatformFinanceRouteEnum,
  ...CityManagerRouteEnum,
  ...CustomFormRouteEnum,
  ...SuggestionRouteEnum,
  ...AiQuotaRouteEnum,
  ...ScriptRouteEnum,
  ...ContentRouteEnum,
  ...PlatformAiQuotaRouteEnum,
  ...PlatformAdminRouteEnum,
};
