// AI 额度管理（平台端，仅 admin）
export const platformAiQuotaCostUrl = '/admin/ai-quota/cost'; // 成本看板
export const platformAiQuotaModelPriceListUrl = '/admin/ai-quota/model-price/list'; // 模型单价列表
export const platformAiQuotaModelPriceSaveUrl = '/admin/ai-quota/model-price/save'; // 模型单价保存
export const platformAiQuotaConfigUrl = '/admin/ai-quota/config'; // 全局配置查询(GET)/更新(POST)
export const platformAiQuotaMockRecordUrl = '/admin/ai-quota/mock-record'; // 模拟记账(验证用，上线前删)
export const platformAiQuotaTenantListUrl = '/admin/ai-quota/tenant/list'; // 租户额度列表
export const platformAiQuotaTenantQuotaSaveUrl = '/admin/ai-quota/tenant/quota/save'; // 按租户调额
export const platformAiQuotaTenantQuotaResetUrl = '/admin/ai-quota/tenant/quota/reset'; // 恢复默认额度
