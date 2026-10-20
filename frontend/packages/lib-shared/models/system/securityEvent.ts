import type { TableQueryParams } from '../common';

export interface SecurityEventListParams extends TableQueryParams {
  eventType?: string | null;
  ip?: string;
  startTime?: number;
  endTime?: number;
}

export interface SecurityEventItem {
  id: string;
  organizationId?: string | null;
  eventType: string;
  remoteAddr?: string;
  clientIp?: string;
  forwardChain?: string;
  ipRegion?: string;
  method?: string;
  uri?: string;
  userAgent?: string;
  referer?: string;
  httpStatus?: number;
  traceId?: string;
  detail?: string;
  createTime?: number;
}
