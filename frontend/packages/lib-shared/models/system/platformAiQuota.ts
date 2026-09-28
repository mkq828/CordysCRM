// AI 额度管理（平台端，仅 admin）

// 成本看板 - 单租户行
export interface AdminAiCostTenantRow {
  organizationId: string;
  organizationName: string;
  costCalls: number | string;
  inputTokens: number;
  outputTokens: number;
  cost: number | string;
}

// 成本看板 - 趋势点
export interface AiCostTrendPoint {
  bucket: string;
  cost: number | string;
}

// 成本看板 - 总览
export interface AdminAiCostOverview {
  totalCost: number | string;
  totalTokens: number;
  totalCalls: number | string;
  tenantRows: AdminAiCostTenantRow[];
  series: AiCostTrendPoint[];
}

// 成本看板 - 查询参数
export interface AdminAiCostRequest {
  groupBy?: 'DAY' | 'MONTH';
  startTime?: number;
  endTime?: number;
}

// 模型单价
export interface AiModelPrice {
  id: string;
  modelCode: string;
  modelName: string;
  inputPrice: number | string;
  outputPrice: number | string;
  status: number;
}

// 模型单价保存参数
export interface AiModelPriceSaveParams {
  id?: string;
  modelCode: string;
  modelName: string;
  inputPrice: number | string;
  outputPrice: number | string;
  status: number;
}

// 全局配置
export interface AiQuotaConfig {
  tokensPerCall: number;
  softLimitPercent: number;
  trialQuota: number;
  minuteCallLimit: number;
  dailyCallLimit: number;
  dailyCostThreshold: number;
}

// 全局配置更新参数
export interface AiQuotaConfigParams {
  tokensPerCall?: number;
  softLimitPercent?: number;
  trialQuota?: number;
  minuteCallLimit?: number;
  dailyCallLimit?: number;
  dailyCostThreshold?: number;
}

// 模拟记账参数（验证用）
export interface AiMockRecordParams {
  organizationId: string;
  featureCode: string;
  modelCode: string;
  inputTokens: number;
  outputTokens: number;
}

// 记账结果
export interface AiQuotaRecordResult {
  status: string;
  costCalls: number | string;
  usedCalls: number | string;
  quota: number;
}
