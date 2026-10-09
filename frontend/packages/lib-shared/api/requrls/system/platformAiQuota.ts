// AI 额度管理（平台端，仅 admin）
export const platformAiQuotaCostUrl = '/admin/ai-quota/cost'; // 成本看板
export const platformAiQuotaModelPriceListUrl = '/admin/ai-quota/model-price/list'; // 模型单价列表
export const platformAiQuotaModelPriceSaveUrl = '/admin/ai-quota/model-price/save'; // 模型单价保存
export const platformAiQuotaConfigUrl = '/admin/ai-quota/config'; // 全局配置查询(GET)/更新(POST)
export const platformAiQuotaMockRecordUrl = '/admin/ai-quota/mock-record'; // 模拟记账(验证用，上线前删)
export const platformAiQuotaTenantListUrl = '/admin/ai-quota/tenant/list'; // 租户额度列表
export const platformAiQuotaTenantQuotaSaveUrl = '/admin/ai-quota/tenant/quota/save'; // 按租户调额
export const platformAiQuotaTenantQuotaResetUrl = '/admin/ai-quota/tenant/quota/reset'; // 恢复默认额度

// AI 额度包（平台端，仅 admin）
export const platformAiQuotaPackListUrl = '/admin/ai-quota-pack/packs'; // 额度包商品列表(含停用)
export const platformAiQuotaPackSaveUrl = '/admin/ai-quota-pack/pack/save'; // 额度包商品保存
export const platformAiQuotaPackDeleteUrl = '/admin/ai-quota-pack/pack/delete'; // 额度包商品删除
export const platformAiQuotaPackOrderListUrl = '/admin/ai-quota-pack/orders'; // 额度包订单列表
export const platformAiQuotaPackOrderConfirmUrl = '/admin/ai-quota-pack/order/confirm'; // 订单确认到账
export const platformAiQuotaPackOrderCancelUrl = '/admin/ai-quota-pack/order/cancel'; // 订单取消
