// 平台城市经理（admin 管理 + city_manager 本人视角）

// 城市经理账号状态
export type CityManagerStatus = 'ENABLED' | 'DISABLED';

// 城市经理列表项
export interface CityManagerItem {
  id: string;
  name: string;
  phone: string;
  status: CityManagerStatus;
  signedCount: number;
  followCount: number;
  createTime: number;
}

// 城市经理视角的租户（组织）
export interface CityManagerOrgItem {
  id: string;
  name: string;
  orgType?: string;
  signManagerId?: string;
  followManagerId?: string;
}

export interface CityManagerAddParams {
  name: string;
  phone: string;
  password?: string;
}

export interface CityManagerPageParams {
  current: number;
  pageSize: number;
  keyword?: string;
  status?: CityManagerStatus | '';
}

export interface CityManagerAssignParams {
  organizationId: string;
  signManagerId?: string;
  followManagerId?: string;
}

export interface CityManagerReassignParams {
  fromManagerId: string;
  toManagerId: string;
  organizationIds: string[];
}

// 批量分配：一次把多家租户的签约/跟进经理批量调整
export interface CityManagerBatchAssignParams {
  items: {
    organizationId: string;
    signManagerId?: string;
    followManagerId?: string;
  }[];
}

// 业绩看板
export interface CityManagerPerformanceSummary {
  managerId: string;
  name: string;
  signedCount: number;
  contractAmount: number | string;
  paymentAmount: number | string;
}

export interface CityManagerPerformancePoint {
  bucket: string;
  signedCount: number;
  contractAmount: number | string;
  paymentAmount: number | string;
}

export interface CityManagerPerformanceOverview {
  summary: CityManagerPerformanceSummary[];
  series: CityManagerPerformancePoint[];
}

export interface CityManagerPerformanceParams {
  managerId?: string;
  groupBy: 'WEEK' | 'MONTH' | 'YEAR';
  startTime?: number;
  endTime?: number;
}
