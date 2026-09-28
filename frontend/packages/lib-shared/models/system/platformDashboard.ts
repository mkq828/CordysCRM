export interface OrgOverviewRow {
  organizationId: string;
  organizationName: string;
  orgType: string;
  accountCount: number;
  customerCount: number;
  clueCount: number;
  opportunityCount: number;
  orderCount: number;
  contractAmount: number | string;
  receivedAmount: number | string;
  outstandingAmount: number | string;
  lastLoginTime?: number;
  active: boolean;
  usageDays: number;
  activeDays30: number;
  lastLoginCity?: string;
  mainLoginCity?: string;
  createTime?: number;
}

export interface PlatformDashboardResponse {
  totalTenant: number;
  enterpriseTenant: number;
  personalTenant: number;
  activeTenant: number;
  totalAccount: number;
  totalClue: number;
  totalCustomer: number;
  totalOpportunity: number;
  totalOrder: number;
  totalContractAmount: number | string;
  totalReceivedAmount: number | string;
  totalOutstandingAmount: number | string;
  rows: OrgOverviewRow[];
}
