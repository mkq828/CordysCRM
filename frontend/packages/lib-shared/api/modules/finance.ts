import type { CordysAxios } from '@lib/shared/api/http/Axios';

import { FinanceOverviewUrl, FinancePageUrl, FinanceRevokeUrl, FinanceVerifyUrl } from '../requrls/finance';

export interface FinanceOverview {
  totalContractAmount: number;
  verifiedAmount: number;
  pendingAmount: number;
  unverifiedAmount: number;
  customerCount: number;
  contractCount: number;
  unsettledContractCount: number;
}

export interface FinancePaymentRecord {
  id: string;
  contractId: string;
  no: string;
  name: string;
  recordAmount: number;
  recordEndTime: number;
  createTime: number;
  verificationStatus: 'PENDING' | 'DONE';
  verifyUser: string;
  verifyUserName: string;
  verifyTime: number;
  verifyRemark: string;
  verifyProof: string;
  revokeUser: string;
  revokeUserName: string;
  revokeTime: number;
  revokeRemark: string;
}

export interface FinanceContract {
  contractId: string;
  contractName: string;
  contractNumber: string;
  amount: number;
  verifiedAmount: number;
  pendingAmount: number;
  settled: boolean;
  createTime: number;
  records: FinancePaymentRecord[];
}

export interface FinanceCustomerGroup {
  customerId: string;
  customerName: string;
  totalAmount: number;
  verifiedAmount: number;
  pendingAmount: number;
  settled: boolean;
  customerCreateTime: number;
  contracts: FinanceContract[];
}

export interface FinancePageResult {
  list: FinanceCustomerGroup[];
  total: number;
  pageSize: number;
  current: number;
}

export interface FinancePageParams {
  current: number;
  pageSize: number;
  keyword?: string;
}

export interface FinanceVerifyParams {
  id: string;
  remark?: string;
  proofAttachmentIds?: string[];
}

export interface FinanceRevokeParams {
  id: string;
  remark?: string;
}

export default function useFinanceApi(CDR: CordysAxios) {
  function financeOverview() {
    return CDR.post<FinanceOverview>({ url: FinanceOverviewUrl, data: {} });
  }

  function financePage(data: FinancePageParams) {
    return CDR.post<FinancePageResult>({ url: FinancePageUrl, data }, { ignoreCancelToken: true });
  }

  function financeVerify(data: FinanceVerifyParams) {
    return CDR.post({ url: FinanceVerifyUrl, data });
  }

  function financeRevoke(data: FinanceRevokeParams) {
    return CDR.post({ url: FinanceRevokeUrl, data });
  }

  return {
    financeOverview,
    financePage,
    financeVerify,
    financeRevoke,
  };
}
