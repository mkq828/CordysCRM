import type { CordysAxios } from '@lib/shared/api/http/Axios';

import {
  BusinessScreenOverviewUrl,
  BusinessScreenRegionDetailUrl,
  BusinessScreenSalesDetailUrl,
  BusinessScreenStageDetailUrl,
} from '../requrls/businessScreen';

export interface BusinessScreenKpi {
  customerCount: number;
  clueCount: number;
  opportunityCount: number;
  orderCount: number;
  contractCount: number;
  totalContractAmount: number;
  verifiedAmount: number;
  pendingAmount: number;
  monthNewCustomer: number;
  monthNewClue: number;
}

export interface TrendPoint {
  month: string;
  amount: number;
}

export interface RankItem {
  owner: string;
  ownerName: string;
  contractAmount: number;
  contractCount: number;
  verifiedAmount: number;
  customerCount: number;
}

export interface DistributionItem {
  code?: string;
  name: string;
  value: number;
}

export interface RegionItem {
  code: string;
  value: number;
}

export interface Funnel {
  clue: number;
  customer: number;
  opportunity: number;
  order: number;
  contract: number;
}

export interface BusinessScreenOverview {
  kpi: BusinessScreenKpi;
  contractTrend: TrendPoint[];
  paymentTrend: TrendPoint[];
  salesRank: RankItem[];
  contractStageDist: DistributionItem[];
  customerRegionDist: RegionItem[];
  funnel: Funnel;
}

export interface SalesKpi {
  contractAmount: number;
  verifiedAmount: number;
  pendingAmount: number;
  contractCount: number;
  customerCount: number;
  clueCount: number;
  opportunityCount: number;
  monthNewCustomer: number;
  monthNewContract: number;
  monthNewContractAmount: number;
}

export interface CustomerBrief {
  customerId: string;
  name: string;
  level: string | null;
  contractAmount: number;
  verifiedAmount: number;
  createTime?: number;
}

export interface SalesOverview {
  kpi: SalesKpi;
  contractTrend: TrendPoint[];
  paymentTrend: TrendPoint[];
  stageDist: DistributionItem[];
  customerList: CustomerBrief[];
}

export interface RegionKpi {
  customerCount: number;
  contractAmount: number;
  verifiedAmount: number;
  pendingAmount: number;
  contractCount: number;
}

export interface RegionDetail {
  kpi: RegionKpi;
  customerList: CustomerBrief[];
  salesDist: RankItem[];
}

export interface StageContractItem {
  contractId: string;
  contractNumber: string;
  customerId: string;
  customerName: string;
  amount: number;
  owner: string;
  ownerName: string;
  signDate: string;
  verifiedAmount: number;
}

export interface StageDetail {
  stageName: string;
  count: number;
  totalAmount: number;
  list: StageContractItem[];
}

export default function useBusinessScreenApi(CDR: CordysAxios) {
  function businessScreenOverview() {
    return CDR.get<BusinessScreenOverview>({ url: BusinessScreenOverviewUrl });
  }

  function businessScreenSalesDetail(ownerId: string) {
    return CDR.get<SalesOverview>({ url: `${BusinessScreenSalesDetailUrl}/${ownerId}` });
  }

  function businessScreenRegionDetail(adcode: string) {
    return CDR.get<RegionDetail>({ url: `${BusinessScreenRegionDetailUrl}/${adcode}` });
  }

  function businessScreenStageDetail(stage: string) {
    return CDR.get<StageDetail>({ url: `${BusinessScreenStageDetailUrl}/${stage}` });
  }

  return {
    businessScreenOverview,
    businessScreenSalesDetail,
    businessScreenRegionDetail,
    businessScreenStageDetail,
  };
}
