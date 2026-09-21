import type { TableQueryParams } from '../common';

export interface BugReportSubmitParams {
  description: string;
  steps?: string;
  screenshot?: string;
  route?: string;
  version?: string;
  roles?: string;
  userAgent?: string;
  screen?: string;
  language?: string;
  recentErrors?: string;
  failedRequests?: string;
  traceId?: string;
}

export interface BugReportListParams extends TableQueryParams {
  status?: string | null;
  keyword?: string;
  startTime?: number;
  endTime?: number;
}

export interface BugReportItem {
  id: string;
  userId: string;
  userName: string;
  route: string;
  description: string;
  traceId: string;
  status: string;
  createTime: number;
}

export interface BugReportDetail extends BugReportItem {
  organizationId: string;
  roles: string;
  version: string;
  userAgent: string;
  screen: string;
  language: string;
  steps: string;
  screenshot: string;
  recentErrors: string;
  failedRequests: string;
  handleTime?: number;
  handleUser?: string;
  handleRemark?: string;
}
