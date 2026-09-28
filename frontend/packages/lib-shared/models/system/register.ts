import type { TableQueryParams } from '../common';

// 注册类型
export type RegisterType = 'PERSONAL' | 'ENTERPRISE';

// 注册审核状态
export type RegisterVerifyStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

// 注册申请提交参数
export interface RegisterApplyParams {
  type: RegisterType;
  name: string;
  phone: string;
  password: string; // RSA 加密后的密码
  idCard: string;
  unifiedSocialCreditCode?: string;
  legalPersonName?: string;
  businessLicenseAttachmentId?: string;
  captchaId?: string;
  captchaCode?: string;
}

// 注册审核状态查询结果
export interface RegisterStatusResult {
  type: RegisterType;
  verifyStatus: RegisterVerifyStatus;
  verifyRemark: string;
}

// 注册申请分页查询参数
export interface RegisterAuditQueryParams extends TableQueryParams {
  type?: RegisterType;
  verifyStatus?: RegisterVerifyStatus;
  keyword?: string;
}

// 注册申请列表项 / 详情
export interface RegisterAuditItem {
  id: string;
  type: RegisterType;
  name: string;
  phone: string;
  idCard?: string; // 脱敏后的身份证号
  unifiedSocialCreditCode?: string;
  legalPersonName?: string;
  businessLicenseAttachmentId?: string;
  verifyStatus: RegisterVerifyStatus;
  verifyRemark?: string;
  verifyUser?: string;
  verifyTime?: number;
  userId?: string;
  planId?: string;
  planVersion?: string;
  planStatus?: string;
  planExpireTime?: number;
  usageDays?: number;
  lastLoginTime?: number;
  enabled?: boolean;
  createTime: number;
}

// 注册申请审核通过参数
export interface RegisterApproveParams {
  id: string;
}

// 注册申请驳回参数
export interface RegisterRejectParams {
  id: string;
  remark: string;
}

// 注册申请账号启用/禁用参数
export interface RegisterToggleParams {
  id: string;
  enabled: boolean;
}
