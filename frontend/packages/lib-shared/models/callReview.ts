import type { SalesAdvisorAnalyzeResult } from './ai';

/** 通话复盘状态：待转写 → 转写中 → 分析中 → 完成 / 失败 */
export type CallReviewStatus = 'PENDING_TRANSCRIBE' | 'TRANSCRIBING' | 'ANALYZING' | 'DONE' | 'FAILED';

/** 通话复盘-列表项 */
export interface CallReviewResponse {
  id: string;
  supplier?: string;
  caller?: string;
  callee?: string;
  customerPhone?: string;
  customerId?: string;
  customerName?: string;
  callTime?: number;
  duration?: number;
  status: CallReviewStatus;
  errorMsg?: string;
  createTime?: number;
}

/** 通话复盘-详情 */
export interface CallReviewDetailResponse extends CallReviewResponse {
  recordUrl?: string;
  transcript?: string;
  review?: SalesAdvisorAnalyzeResult | null;
  updateTime?: number;
}

/** 通话复盘-上传/粘贴录音发起复盘请求 */
export interface CallReviewUploadRequest {
  recordUrl?: string;
  recordAttachmentId?: string;
  customerId?: string;
  caller?: string;
  callee?: string;
  customerPhone?: string;
  callTime?: number;
  duration?: number;
}

/** 通话复盘-分页查询参数 */
export interface CallReviewPageRequest {
  current?: number;
  pageSize?: number;
  status?: string;
}

/** 通话复盘-回调配置 */
export interface CallReviewConfigResponse {
  appKey: string;
  secretKey: string;
  callbackUrl: string;
  fieldMapping: Record<string, string>;
  enable: boolean;
}

/** 通话复盘-回调配置保存请求 */
export interface CallReviewConfigSaveRequest {
  fieldMapping: Record<string, string>;
  enable: boolean;
}

/** 回调字段映射：CRM 标准字段 + 供应商字段名 */
export interface CallReviewStandardField {
  key: string;
  label: string;
  required?: boolean;
}
