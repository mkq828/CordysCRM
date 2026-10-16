import type { CordysAxios } from '@lib/shared/api/http/Axios';

import {
  BusinessScreenAlertsUrl,
  BusinessScreenAttributionUrl,
  BusinessScreenBriefingGenerateUrl,
  BusinessScreenBriefingLatestUrl,
  BusinessScreenOverviewUrl,
  BusinessScreenRegionDetailUrl,
  BusinessScreenSalesDetailUrl,
  BusinessScreenStageDetailUrl,
  BusinessScreenSupervisionUrl,
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

export interface AlertItem {
  type: string;
  level: string;
  title: string;
  desc: string;
  targetKind: string;
  targetKey?: string;
  entityId?: string;
  entityName?: string;
  lastFollowTime?: number | null;
  currentAmount?: number;
  previousAmount?: number;
}

export interface AlertResponse {
  items: AlertItem[];
}

export interface AttributionRequest {
  kind: string;
  key?: string;
  range?: string;
}

export interface AttributionResponse {
  content: string;
}

export interface SupervisionCreateRequest {
  assignee: string;
  content: string;
  deadline: number;
}

export interface SupervisionItem {
  id: string;
  assignee: string;
  assigneeName: string;
  content: string;
  deadline: number;
  status: string;
  createUser: string;
  createTime: number;
}

export interface AssigneeOption {
  owner: string;
  ownerName: string;
}

export interface SupervisionBoardResponse {
  items: SupervisionItem[];
  assignees: AssigneeOption[];
}

export interface BriefingResponse {
  id: string;
  type: string;
  period: string;
  content: string;
  createTime: number;
}

export default function useBusinessScreenApi(CDR: CordysAxios) {
  function businessScreenOverview(range?: string) {
    return CDR.get<BusinessScreenOverview>({ url: BusinessScreenOverviewUrl, params: { range } });
  }

  function businessScreenSalesDetail(ownerId: string, range?: string) {
    return CDR.get<SalesOverview>({ url: `${BusinessScreenSalesDetailUrl}/${ownerId}`, params: { range } });
  }

  function businessScreenRegionDetail(adcode: string, range?: string) {
    return CDR.get<RegionDetail>({ url: `${BusinessScreenRegionDetailUrl}/${adcode}`, params: { range } });
  }

  function businessScreenStageDetail(stage: string, range?: string) {
    return CDR.get<StageDetail>({ url: `${BusinessScreenStageDetailUrl}/${stage}`, params: { range } });
  }

  function businessScreenAlerts() {
    return CDR.get<AlertResponse>({ url: BusinessScreenAlertsUrl });
  }

  function businessScreenAttribution(payload: AttributionRequest) {
    return CDR.post<AttributionResponse>({ url: BusinessScreenAttributionUrl, data: payload });
  }

  function businessScreenSupervisionBoard() {
    return CDR.get<SupervisionBoardResponse>({ url: BusinessScreenSupervisionUrl });
  }

  function businessScreenSupervisionCreate(payload: SupervisionCreateRequest) {
    return CDR.post<SupervisionItem>({ url: BusinessScreenSupervisionUrl, data: payload });
  }

  function businessScreenSupervisionDone(id: string) {
    return CDR.put({ url: `${BusinessScreenSupervisionUrl}/${id}/done` });
  }

  function businessScreenBriefingLatest() {
    return CDR.get<BriefingResponse | null>({ url: BusinessScreenBriefingLatestUrl });
  }

  function businessScreenBriefingGenerate(type: string) {
    return CDR.post<BriefingResponse>({ url: BusinessScreenBriefingGenerateUrl, params: { type } });
  }

  return {
    businessScreenOverview,
    businessScreenSalesDetail,
    businessScreenRegionDetail,
    businessScreenStageDetail,
    businessScreenAlerts,
    businessScreenAttribution,
    businessScreenSupervisionBoard,
    businessScreenSupervisionCreate,
    businessScreenSupervisionDone,
    businessScreenBriefingLatest,
    businessScreenBriefingGenerate,
  };
}
