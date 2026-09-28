import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  platformAiQuotaConfigUrl,
  platformAiQuotaCostUrl,
  platformAiQuotaMockRecordUrl,
  platformAiQuotaModelPriceListUrl,
  platformAiQuotaModelPriceSaveUrl,
} from '@lib/shared/api/requrls/system/platformAiQuota';
import type {
  AdminAiCostOverview,
  AdminAiCostRequest,
  AiModelPrice,
  AiModelPriceSaveParams,
  AiQuotaConfig,
  AiQuotaConfigParams,
  AiMockRecordParams,
  AiQuotaRecordResult,
} from '@lib/shared/models/system/platformAiQuota';

export default function usePlatformAiQuotaApi(CDR: CordysAxios) {
  // 成本看板
  function cost(data: AdminAiCostRequest) {
    return CDR.post<AdminAiCostOverview>({ url: platformAiQuotaCostUrl, data });
  }

  // 模型单价
  function modelPriceList() {
    return CDR.get<AiModelPrice[]>({ url: platformAiQuotaModelPriceListUrl });
  }
  function modelPriceSave(data: AiModelPriceSaveParams) {
    return CDR.post({ url: platformAiQuotaModelPriceSaveUrl, data });
  }

  // 全局配置
  function getConfig() {
    return CDR.get<AiQuotaConfig>({ url: platformAiQuotaConfigUrl });
  }
  function updateConfig(data: AiQuotaConfigParams) {
    return CDR.post({ url: platformAiQuotaConfigUrl, data });
  }

  // 模拟记账（验证用，上线前删）
  function mockRecord(data: AiMockRecordParams) {
    return CDR.post<AiQuotaRecordResult>({ url: platformAiQuotaMockRecordUrl, data });
  }

  return {
    cost,
    modelPriceList,
    modelPriceSave,
    getConfig,
    updateConfig,
    mockRecord,
  };
}
