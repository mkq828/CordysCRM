import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  platformAiQuotaConfigUrl,
  platformAiQuotaCostUrl,
  platformAiQuotaMockRecordUrl,
  platformAiQuotaModelPriceListUrl,
  platformAiQuotaModelPriceSaveUrl,
  platformAiQuotaTenantListUrl,
  platformAiQuotaTenantQuotaResetUrl,
  platformAiQuotaTenantQuotaSaveUrl,
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
  AiTenantQuotaListParams,
  AiTenantQuotaResetParams,
  AiTenantQuotaRow,
  AiTenantQuotaSaveParams,
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

  // 租户额度
  function tenantList(data: AiTenantQuotaListParams) {
    return CDR.post<AiTenantQuotaRow[]>({ url: platformAiQuotaTenantListUrl, data });
  }
  function tenantQuotaSave(data: AiTenantQuotaSaveParams) {
    return CDR.post({ url: platformAiQuotaTenantQuotaSaveUrl, data });
  }
  function tenantQuotaReset(data: AiTenantQuotaResetParams) {
    return CDR.post({ url: platformAiQuotaTenantQuotaResetUrl, data });
  }

  return {
    cost,
    modelPriceList,
    modelPriceSave,
    getConfig,
    updateConfig,
    mockRecord,
    tenantList,
    tenantQuotaSave,
    tenantQuotaReset,
  };
}
