import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  platformBankAccountListUrl,
  platformBankAccountSaveUrl,
  platformConfigUrl,
  platformContractAddUrl,
  platformContractDeleteUrl,
  platformContractGetUrl,
  platformContractListUrl,
  platformContractOptionsUrl,
  platformContractOrgOptionsUrl,
  platformContractStatusUrl,
  platformContractUpdateUrl,
  platformInvoiceAddUrl,
  platformInvoiceDeleteUrl,
  platformInvoiceInvoiceUrl,
  platformInvoiceListUrl,
  platformInvoiceUpdateUrl,
  platformInvoiceVoidUrl,
  platformPaymentRecordAddUrl,
  platformPaymentRecordDeleteUrl,
  platformPaymentRecordListUrl,
  platformPaymentRecordRevokeUrl,
  platformPaymentRecordUpdateUrl,
  platformPaymentRecordVerifyUrl,
  platformRevenueOverviewUrl,
} from '@lib/shared/api/requrls/system/platformFinance';
import type { CommonList } from '@lib/shared/models/common';
import type {
  PlatformBankAccount,
  PlatformConfig,
  PlatformContractDetail,
  PlatformContractItem,
  PlatformContractOption,
  PlatformContractQueryParams,
  PlatformContractSaveParams,
  PlatformContractStatusParams,
  PlatformOrgOption,
  PlatformInvoiceActionParams,
  PlatformInvoiceItem,
  PlatformInvoiceQueryParams,
  PlatformInvoiceSaveParams,
  PlatformPaymentRecordItem,
  PlatformPaymentRecordQueryParams,
  PlatformPaymentRecordSaveParams,
  PlatformRevenueOverview,
  PlatformRevenueQueryParams,
  PlatformRevokeParams,
  PlatformVerifyParams,
} from '@lib/shared/models/system/platformFinance';

export default function usePlatformFinanceApi(CDR: CordysAxios) {
  // 平台合同
  function pageList(data: PlatformContractQueryParams) {
    return CDR.post<CommonList<PlatformContractItem>>({ url: platformContractListUrl, data });
  }
  function get(id: string) {
    return CDR.get<PlatformContractDetail>({ url: `${platformContractGetUrl}/${id}` });
  }
  function orgOptions() {
    return CDR.get<PlatformOrgOption[]>({ url: platformContractOrgOptionsUrl });
  }
  function options() {
    return CDR.get<PlatformContractOption[]>({ url: platformContractOptionsUrl });
  }
  function add(data: PlatformContractSaveParams) {
    return CDR.post({ url: platformContractAddUrl, data });
  }
  function update(data: PlatformContractSaveParams) {
    return CDR.post({ url: platformContractUpdateUrl, data });
  }
  function remove(id: string) {
    return CDR.get({ url: `${platformContractDeleteUrl}/${id}` });
  }
  function changeStatus(data: PlatformContractStatusParams) {
    return CDR.post({ url: platformContractStatusUrl, data });
  }

  // 平台回款
  function paymentPageList(data: PlatformPaymentRecordQueryParams) {
    return CDR.post<CommonList<PlatformPaymentRecordItem>>({ url: platformPaymentRecordListUrl, data });
  }
  function paymentAdd(data: PlatformPaymentRecordSaveParams) {
    return CDR.post({ url: platformPaymentRecordAddUrl, data });
  }
  function paymentUpdate(data: PlatformPaymentRecordSaveParams) {
    return CDR.post({ url: platformPaymentRecordUpdateUrl, data });
  }
  function paymentRemove(id: string) {
    return CDR.get({ url: `${platformPaymentRecordDeleteUrl}/${id}` });
  }
  function verify(data: PlatformVerifyParams) {
    return CDR.post({ url: platformPaymentRecordVerifyUrl, data });
  }
  function revoke(data: PlatformRevokeParams) {
    return CDR.post({ url: platformPaymentRecordRevokeUrl, data });
  }

  // 平台发票
  function invoicePageList(data: PlatformInvoiceQueryParams) {
    return CDR.post<CommonList<PlatformInvoiceItem>>({ url: platformInvoiceListUrl, data });
  }
  function invoiceAdd(data: PlatformInvoiceSaveParams) {
    return CDR.post({ url: platformInvoiceAddUrl, data });
  }
  function invoiceUpdate(data: PlatformInvoiceSaveParams) {
    return CDR.post({ url: platformInvoiceUpdateUrl, data });
  }
  function invoiceRemove(id: string) {
    return CDR.get({ url: `${platformInvoiceDeleteUrl}/${id}` });
  }
  function invoice(data: PlatformInvoiceActionParams) {
    return CDR.post({ url: platformInvoiceInvoiceUrl, data });
  }
  function voidInvoice(data: PlatformInvoiceActionParams) {
    return CDR.post({ url: platformInvoiceVoidUrl, data });
  }

  // 平台营收看板
  function revenueOverview(data: PlatformRevenueQueryParams) {
    return CDR.post<PlatformRevenueOverview>({ url: platformRevenueOverviewUrl, data });
  }

  // 平台收款设置
  function getConfig() {
    return CDR.get<PlatformConfig>({ url: platformConfigUrl });
  }
  function updateConfig(data: PlatformConfig) {
    return CDR.post({ url: platformConfigUrl, data });
  }

  // 平台收款账号
  function bankAccountList() {
    return CDR.get<PlatformBankAccount[]>({ url: platformBankAccountListUrl });
  }
  function bankAccountSave(data: PlatformBankAccount[]) {
    return CDR.post({ url: platformBankAccountSaveUrl, data });
  }

  return {
    pageList,
    get,
    orgOptions,
    options,
    add,
    update,
    remove,
    changeStatus,
    paymentPageList,
    paymentAdd,
    paymentUpdate,
    paymentRemove,
    verify,
    revoke,
    invoicePageList,
    invoiceAdd,
    invoiceUpdate,
    invoiceRemove,
    invoice,
    voidInvoice,
    revenueOverview,
    getConfig,
    updateConfig,
    bankAccountList,
    bankAccountSave,
  };
}
