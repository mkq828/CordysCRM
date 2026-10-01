import type { TableQueryParams } from '../common';

// 平台合同状态机
export type PlatformContractStatus = 'DRAFT' | 'PENDING_SIGN' | 'COMPLETED' | 'ARCHIVED' | 'VOIDED';

// 平台回款核销状态
export type PlatformPaymentVerificationStatus = 'PENDING' | 'DONE';

// 平台发票开票状态
export type PlatformInvoiceStatus = 'NOT_INVOICED' | 'INVOICED' | 'VOIDED';

export interface PlatformAttachment {
  id: string;
  name: string;
  type?: string;
  size?: number;
}

// 租户（组织）下拉选项
export interface PlatformOrgOption {
  id: string;
  name: string;
  orgType?: string;
  // 租户当前套餐版本 code，选中租户后默认带出
  editionCode?: string;
  // 是否已有有效合同（草稿/作废不计），用于首份合同用首年价、续约用年价
  hasContract?: boolean;
}

// 平台合同下拉选项
export interface PlatformContractOption {
  id: string;
  contractNo: string;
  orgName?: string;
  editionName?: string;
  amount?: number | string;
}

// 平台合同列表项
export interface PlatformContractItem {
  id: string;
  contractNo: string;
  organizationId: string;
  orgName?: string;
  creditCode?: string;
  contactPerson?: string;
  contactPhone?: string;
  address?: string;
  editionCode?: string;
  editionName?: string;
  amount?: number | string;
  validityDays?: number;
  signType?: string;
  signManagerId?: string;
  followManagerId?: string;
  signManagerName?: string;
  followManagerName?: string;
  status: PlatformContractStatus;
  attachmentIds?: string;
  remark?: string;
  createTime?: number;
  createUser?: string;
  updateTime?: number;
  updateUser?: string;
  attachmentList?: PlatformAttachment[];
}

export interface PlatformContractDetail extends PlatformContractItem {
  attachmentList?: PlatformAttachment[];
}

// 平台回款列表项
export interface PlatformPaymentRecordItem {
  id: string;
  contractId: string;
  organizationId: string;
  recordNo?: string;
  amount?: number | string;
  paymentType?: string;
  bankAccount?: string;
  voucherAttachmentIds?: string;
  verificationStatus: PlatformPaymentVerificationStatus;
  verifyUser?: string;
  verifyTime?: number;
  verifyRemark?: string;
  verifyProof?: string;
  revokeUser?: string;
  revokeTime?: number;
  revokeRemark?: string;
  remark?: string;
  createTime?: number;
  createUser?: string;
  contractNo?: string;
  orgName?: string;
  signManagerName?: string;
  followManagerName?: string;
  voucherList?: PlatformAttachment[];
  proofList?: PlatformAttachment[];
}

// 平台发票列表项
export interface PlatformInvoiceItem {
  id: string;
  contractId: string;
  organizationId: string;
  invoiceNo?: string;
  invoiceType?: string;
  amount?: number | string;
  taxRate?: number | string;
  taxAmount?: number | string;
  invoiceStatus: PlatformInvoiceStatus;
  businessTitle?: string;
  remark?: string;
  createTime?: number;
  createUser?: string;
  updateTime?: number;
  updateUser?: string;
  contractNo?: string;
  orgName?: string;
  signManagerName?: string;
  followManagerName?: string;
}

export interface PlatformRevenuePoint {
  bucket: string;
  contractAmount: number | string;
  receivedAmount: number | string;
  invoiceAmount: number | string;
}

export interface PlatformRevenueOverview {
  contractAmount: number | string;
  receivedAmount: number | string;
  outstandingAmount: number | string;
  invoiceAmount: number | string;
  series: PlatformRevenuePoint[];
}

export interface PlatformConfig {
  companyName?: string;
  invoiceTitle?: string;
  taxRate?: string;
}

// 平台系统信息（关于弹窗）：运营方名称
export interface PlatformSystemInfo {
  operator?: string;
}

// 平台收款账号：我方各收款方式的收款账户（每种方式一条）
export interface PlatformBankAccount {
  id?: string;
  accountType: string;
  accountName?: string;
  accountNo?: string;
  bankName?: string;
  qrcode?: string;
}

// 平台合同分页查询参数
export interface PlatformContractQueryParams extends TableQueryParams {
  status?: PlatformContractStatus;
}

// 平台回款分页查询参数
export interface PlatformPaymentRecordQueryParams extends TableQueryParams {
  contractId?: string;
  verificationStatus?: PlatformPaymentVerificationStatus;
}

// 平台发票分页查询参数
export interface PlatformInvoiceQueryParams extends TableQueryParams {
  contractId?: string;
  invoiceStatus?: PlatformInvoiceStatus;
}

// 平台营收看板查询参数
export interface PlatformRevenueQueryParams {
  startTime?: number;
  endTime?: number;
  groupBy?: 'WEEK' | 'MONTH' | 'YEAR';
}

// 平台合同保存参数
export interface PlatformContractSaveParams {
  id?: string;
  contractNo: string;
  organizationId: string;
  creditCode?: string;
  contactPerson?: string;
  contactPhone?: string;
  address?: string;
  editionCode?: string;
  amount?: number | string;
  validityDays?: number;
  signType?: string;
  signManagerId?: string;
  attachmentIds?: string;
  remark?: string;
}

export interface PlatformContractStatusParams {
  id: string;
  status: PlatformContractStatus;
}

// 平台回款保存参数
export interface PlatformPaymentRecordSaveParams {
  id?: string;
  contractId: string;
  recordNo?: string;
  amount?: number | string;
  paymentType?: string;
  bankAccount?: string;
  voucherAttachmentIds?: string;
  remark?: string;
}

export interface PlatformVerifyParams {
  id: string;
  remark?: string;
  proofAttachmentIds?: string[];
}

export interface PlatformRevokeParams {
  id: string;
  remark?: string;
}

// 平台发票保存参数
export interface PlatformInvoiceSaveParams {
  id?: string;
  contractId: string;
  invoiceNo?: string;
  invoiceType?: string;
  amount?: number | string;
  taxRate?: number | string;
  businessTitle?: string;
  remark?: string;
}

export interface PlatformInvoiceActionParams {
  id: string;
  invoiceNo?: string;
}
