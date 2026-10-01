import type { TableQueryParams } from '../common';

// 套餐版本（版本编码，读 sys_edition，如 BASIC/PRO/ENTERPRISE）
export type TenantPlanVersion = string;

// 套餐状态
export type TenantPlanStatus = 'FREE' | 'ACTIVE' | 'EXPIRED';

// 套餐分页查询参数
export interface TenantPlanQueryParams extends TableQueryParams {
  version?: TenantPlanVersion;
  status?: TenantPlanStatus;
  keyword?: string;
}

// 套餐列表项
export interface TenantPlanItem {
  id: string;
  organizationId: string;
  version: TenantPlanVersion;
  status: TenantPlanStatus;
  expireTime?: number;
  price?: number;
  remark?: string;
  orgName: string;
  orgType: TenantPlanVersion;
  phone?: string;
  enabled?: boolean;
  demo?: boolean;
  lastLoginTime?: number;
  remainingDays?: number;
  signManagerId?: string;
  followManagerId?: string;
  signManagerName?: string;
  followManagerName?: string;
  createTime: number;
  updateTime?: number;
}

// 开通/续费参数
export interface TenantPlanOpenParams {
  id: string;
  version: TenantPlanVersion;
  expireTime?: number;
  remark?: string;
}

// 升级企业版参数
export interface TenantPlanUpgradeParams {
  id: string;
  editionCode: TenantPlanVersion;
}

// 账号启用/禁用参数
export interface TenantPlanToggleParams {
  organizationId: string;
  enabled: boolean;
}

// 演示标记参数
export interface TenantPlanDemoParams {
  organizationId: string;
  demo: boolean;
}

// 全局配置
export interface TenantPlanConfig {
  freeTrialDays: number;
  expireRemindDays: string;
  graceDays?: number;
}

// 租户详情查询参数
export interface TenantPlanDetailParams {
  organizationId: string;
}

// 开通/续费/升级历史条目
export interface TenantPlanHistoryItem {
  action: 'OPEN' | 'UPGRADE';
  fromVersion?: string;
  toVersion: string;
  fromVersionName?: string;
  toVersionName?: string;
  price?: number;
  priceDetail?: string;
  expireTime?: number;
  remark?: string;
  createTime?: number;
  createUser?: string;
}

// 租户详情
export interface TenantPlanDetail {
  organizationId: string;
  orgName: string;
  orgType: string;
  unifiedSocialCreditCode?: string;
  legalPersonName?: string;
  demo?: boolean;
  adminName?: string;
  phone?: string;
  enabled?: boolean;
  version?: string;
  editionName?: string;
  status?: TenantPlanStatus;
  price?: number;
  expireTime?: number;
  remainingDays?: number;
  aiQuota?: number;
  aiUsedCalls?: number;
  histories?: TenantPlanHistoryItem[];
}

// 续费/升级报价类型
export type TenantPlanQuoteType = 'RENEW' | 'UPGRADE' | 'DOWNGRADE';

// 续费/升级报价
export interface TenantPlanQuote {
  type: TenantPlanQuoteType;
  currentVersion?: string;
  currentVersionName?: string;
  targetVersion: string;
  targetVersionName: string;
  remainDays?: number;
  validityDays?: number;
  amount?: number;
  priceDetail?: string;
}

// 续费/升级申请状态
export type TenantPlanApplicationStatus = 'PENDING' | 'APPROVED' | 'CANCELLED';

// 续费/升级申请（租户/管理端共用）
export interface TenantPlanApplicationItem {
  id: string;
  organizationId: string;
  orgName: string;
  currentVersion?: string;
  currentVersionName?: string;
  targetVersion: string;
  targetVersionName?: string;
  amount?: number;
  priceDetail?: string;
  validityDays?: number;
  paymentType: string;
  voucherIds?: string;
  status: TenantPlanApplicationStatus;
  contractId?: string;
  remark?: string;
  verifyRemark?: string;
  accountName?: string;
  accountNo?: string;
  bankName?: string;
  qrcode?: string;
  createTime: number;
}

// 提交续费/升级申请参数
export interface TenantPlanApplyParams {
  targetEdition: string;
  paymentType: string;
  voucherIds?: string;
  remark?: string;
}

// 核销/驳回申请参数
export interface TenantPlanApplicationApproveParams {
  id: string;
  contractId?: string;
  remark?: string;
}

// 申请分页查询参数（管理端）
export interface TenantPlanApplicationQueryParams extends TableQueryParams {
  status?: TenantPlanApplicationStatus;
  keyword?: string;
}
