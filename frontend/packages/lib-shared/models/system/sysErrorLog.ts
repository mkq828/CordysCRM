import type { TableQueryParams } from '../common';

export interface SysErrorLogListParams extends TableQueryParams {
  loggerName?: string;
  traceId?: string;
  keyword?: string;
  startTime?: number;
  endTime?: number;
}

export interface SysErrorLogItem {
  id: string;
  traceId?: string | null;
  loggerName: string;
  message?: string | null;
  stackTrace?: string | null;
  createTime?: number;
}
