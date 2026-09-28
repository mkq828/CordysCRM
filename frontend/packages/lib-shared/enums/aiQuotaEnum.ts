// AI 额度框架枚举

// 用量/成本趋势分组粒度
export enum AiQuotaGroupByEnum {
  DAY = 'DAY',
  MONTH = 'MONTH',
}

// 记账状态（与后端 AiQuotaConstant.STATUS_* 对应）
export enum AiQuotaStatusEnum {
  NORMAL = 'NORMAL',
  SOFT_LIMITED = 'SOFT_LIMITED',
  HARD_LIMITED = 'HARD_LIMITED',
  RATE_LIMITED = 'RATE_LIMITED',
  CIRCUIT_BROKEN = 'CIRCUIT_BROKEN',
}
