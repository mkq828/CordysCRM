// 平台收费管理（平台账，仅 admin）
export const platformContractListUrl = '/platform/contract/list'; // 平台合同-列表
export const platformContractGetUrl = '/platform/contract/get'; // 平台合同-详情（拼 /{id}）
export const platformContractAddUrl = '/platform/contract/add'; // 平台合同-新增
export const platformContractUpdateUrl = '/platform/contract/update'; // 平台合同-编辑
export const platformContractDeleteUrl = '/platform/contract/delete'; // 平台合同-删除（拼 /{id}）
export const platformContractStatusUrl = '/platform/contract/status'; // 平台合同-状态流转
export const platformContractOrgOptionsUrl = '/platform/contract/org-options'; // 平台合同-租户下拉选项
export const platformContractOptionsUrl = '/platform/contract/options'; // 平台合同-下拉选项

export const platformPaymentRecordListUrl = '/platform/payment-record/list'; // 平台回款-列表
export const platformPaymentRecordAddUrl = '/platform/payment-record/add'; // 平台回款-新增
export const platformPaymentRecordUpdateUrl = '/platform/payment-record/update'; // 平台回款-编辑
export const platformPaymentRecordDeleteUrl = '/platform/payment-record/delete'; // 平台回款-删除（拼 /{id}）
export const platformPaymentRecordVerifyUrl = '/platform/payment-record/verify'; // 平台回款-核销
export const platformPaymentRecordRevokeUrl = '/platform/payment-record/revoke'; // 平台回款-撤回

export const platformInvoiceListUrl = '/platform/invoice/list'; // 平台发票-列表
export const platformInvoiceAddUrl = '/platform/invoice/add'; // 平台发票-新增
export const platformInvoiceUpdateUrl = '/platform/invoice/update'; // 平台发票-编辑
export const platformInvoiceDeleteUrl = '/platform/invoice/delete'; // 平台发票-删除（拼 /{id}）
export const platformInvoiceInvoiceUrl = '/platform/invoice/invoice'; // 平台发票-开票
export const platformInvoiceVoidUrl = '/platform/invoice/void'; // 平台发票-作废

export const platformRevenueOverviewUrl = '/platform/revenue/overview'; // 平台营收看板-总览
export const platformConfigUrl = '/platform/config'; // 平台收款设置-查询(GET)/保存(POST)
export const platformSystemInfoUrl = '/platform/system-info'; // 平台系统信息-查询(GET)/保存(POST)
export const platformBankAccountListUrl = '/platform/bank-account/list'; // 平台收款账号-列表
export const platformBankAccountSaveUrl = '/platform/bank-account/save'; // 平台收款账号-保存
