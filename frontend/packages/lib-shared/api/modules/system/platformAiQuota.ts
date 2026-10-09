import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  platformAiQuotaConfigUrl,
  platformAiQuotaCostUrl,
  platformAiQuotaMockRecordUrl,
  platformAiQuotaModelPriceListUrl,
  platformAiQuotaModelPriceSaveUrl,
  platformAiQuotaPackDeleteUrl,
  platformAiQuotaPackListUrl,
  platformAiQuotaPackOrderCancelUrl,
  platformAiQuotaPackOrderConfirmUrl,
  platformAiQuotaPackOrderListUrl,
  platformAiQuotaPackSaveUrl,
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
  AiQuotaPack,
  AiQuotaPackOrder,
  AiQuotaPackOrderHandleParams,
  AiQuotaPackOrderListParams,
  AiQuotaPackSaveParams,
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

  // 额度包商品
  function packList() {
    return CDR.get<AiQuotaPack[]>({ url: platformAiQuotaPackListUrl });
  }
  function packSave(data: AiQuotaPackSaveParams) {
    return CDR.post({ url: platformAiQuotaPackSaveUrl, data });
  }
  function packDelete(id: string) {
    return CDR.post({ url: platformAiQuotaPackDeleteUrl, params: { id } });
  }

  // 额度包订单
  function packOrderList(data: AiQuotaPackOrderListParams) {
    return CDR.post<AiQuotaPackOrder[]>({ url: platformAiQuotaPackOrderListUrl, data });
  }
  function packOrderConfirm(data: AiQuotaPackOrderHandleParams) {
    return CDR.post({ url: platformAiQuotaPackOrderConfirmUrl, data });
  }
  function packOrderCancel(data: AiQuotaPackOrderHandleParams) {
    return CDR.post({ url: platformAiQuotaPackOrderCancelUrl, data });
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
    packList,
    packSave,
    packDelete,
    packOrderList,
    packOrderConfirm,
    packOrderCancel,
  };
}
