import type { CordysAxios } from '@lib/shared/api/http/Axios';

import { aiQuotaFeatureUsageUrl, aiQuotaOverviewUrl, aiQuotaTrendUrl } from '../requrls/aiQuota';

export interface TenantQuotaOverview {
  period: string;
  usedCalls: number | string;
  quota: number;
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

  return {
    overview,
    trend,
    featureUsage,
  };
}
