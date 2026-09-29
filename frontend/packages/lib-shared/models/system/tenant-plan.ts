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
