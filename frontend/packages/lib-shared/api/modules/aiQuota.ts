import type { CordysAxios } from '@lib/shared/api/http/Axios';

import {
  aiQuotaFeatureUsageUrl,
  aiQuotaOverviewUrl,
  aiQuotaPackApplyUrl,
  aiQuotaPackBalanceUrl,
  aiQuotaPackListUrl,
  aiQuotaPackOrdersUrl,
  aiQuotaTrendUrl,
} from '../requrls/aiQuota';

export interface TenantQuotaOverview {
  period: string;
  usedCalls: number | string;
  quota: number;
  packBalance: number | string;
  remainingCalls: number | string;
  usedPercent: number | string;
  softLimitPercent: number;
  todayCalls: number | string;
  status: string;
}

export interface AiQuotaTrendPoint {
  bucket: string;
  usedCalls: number | string;
}

export interface AiFeatureUsagePoint {
  featureCode: string;
  featureName: string;
  usedCalls: number | string;
}

export interface AiQuotaPackItem {
  id: string;
  featureCode: string;
  name: string;
  calls: number;
  priceFen: number;
  status: number;
  sort: number;
  remark?: string;
  createTime?: number;
}

export interface AiQuotaPackBalance {
  featureCode: string;
  featureName: string;
  remainingCalls: number | string;
}

export interface AiQuotaPackOrder {
  id: string;
  organizationId?: string;
  organizationName?: string;
  packName: string;
  featureCode: string;
  featureName: string;
  calls: number;
  amountFen: number;
  status: string;
  applyTime?: number;
  confirmTime?: number;
  remark?: string;
  createTime?: number;
}

export default function useAiQuotaApi(CDR: CordysAxios) {
  function overview() {
    return CDR.get<TenantQuotaOverview>({ url: aiQuotaOverviewUrl });
  }

  function trend(groupBy?: 'DAY' | 'MONTH') {
    return CDR.get<AiQuotaTrendPoint[]>({ url: aiQuotaTrendUrl, params: { groupBy } });
  }

  function featureUsage() {
    return CDR.get<AiFeatureUsagePoint[]>({ url: aiQuotaFeatureUsageUrl });
  }

  function packList() {
    return CDR.get<AiQuotaPackItem[]>({ url: aiQuotaPackListUrl });
  }

  function packBalance() {
    return CDR.get<AiQuotaPackBalance[]>({ url: aiQuotaPackBalanceUrl });
  }

  function packApply(packId: string) {
    return CDR.post({ url: aiQuotaPackApplyUrl, data: { packId } });
  }

  function packOrders(status?: string) {
    return CDR.get<AiQuotaPackOrder[]>({ url: aiQuotaPackOrdersUrl, params: { status } });
  }

  return {
    overview,
    trend,
    featureUsage,
    packList,
    packBalance,
    packApply,
    packOrders,
  };
}
