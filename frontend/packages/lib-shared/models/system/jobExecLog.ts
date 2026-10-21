import type { TableQueryParams } from '../common';

export interface JobExecLogListParams extends TableQueryParams {
  jobName?: string;
  status?: string | null;
  startTime?: number;
  endTime?: number;
}

export interface JobExecLogItem {
  id: string;
  jobName: string;
  jobGroup?: string | null;
  status: string;
  startTime?: number | null;
  endTime?: number | null;
  durationMs?: number | null;
  errorMsg?: string | null;
  createTime?: number;
}
