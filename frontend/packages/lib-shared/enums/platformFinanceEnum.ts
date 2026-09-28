// 平台收费管理（平台账）枚举

// 合同状态机：草稿 → 待签署 → 已完成 → 已归档 / 已作废
export enum PlatformContractStatusEnum {
  DRAFT = 'DRAFT',
  PENDING_SIGN = 'PENDING_SIGN',
  COMPLETED = 'COMPLETED',
  ARCHIVED = 'ARCHIVED',
  VOIDED = 'VOIDED',
}

// 回款核销状态
export enum PlatformPaymentVerificationStatusEnum {
  PENDING = 'PENDING',
  DONE = 'DONE',
}

// 开票状态
export enum PlatformInvoiceStatusEnum {
  NOT_INVOICED = 'NOT_INVOICED',
  INVOICED = 'INVOICED',
  VOIDED = 'VOIDED',
}

// 收款方式
export enum PlatformPaymentTypeEnum {
  TRANSFER = 'TRANSFER',
  ALIPAY = 'ALIPAY',
  WECHAT = 'WECHAT',
  OFFLINE = 'OFFLINE',
}

// 发票类型
export enum PlatformInvoiceTypeEnum {
  NORMAL = 'NORMAL',
  SPECIAL = 'SPECIAL',
}

// 签署方式
export enum PlatformSignTypeEnum {
  OFFLINE = 'OFFLINE',
  ONLINE = 'ONLINE',
}

// 营收看板分组粒度
export enum PlatformRevenueGroupByEnum {
  WEEK = 'WEEK',
  MONTH = 'MONTH',
  YEAR = 'YEAR',
}

// 合同状态 → n-tag type
export const PlatformContractStatusTagMap: Record<string, 'default' | 'info' | 'warning' | 'success' | 'error'> = {
  DRAFT: 'default',
  PENDING_SIGN: 'warning',
  COMPLETED: 'success',
  ARCHIVED: 'info',
  VOIDED: 'error',
};

// 核销状态 → n-tag type
export const PlatformPaymentVerificationStatusTagMap: Record<string, 'warning' | 'success'> = {
  PENDING: 'warning',
  DONE: 'success',
};

// 开票状态 → n-tag type
export const PlatformInvoiceStatusTagMap: Record<string, 'default' | 'success' | 'error'> = {
  NOT_INVOICED: 'default',
  INVOICED: 'success',
  VOIDED: 'error',
};
