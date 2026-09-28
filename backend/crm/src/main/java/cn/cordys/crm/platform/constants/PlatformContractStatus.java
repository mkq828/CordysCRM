package cn.cordys.crm.platform.constants;

/**
 * 平台合同状态机：草稿 → 待签署 → 已完成 → 已归档 / 已作废
 */
public enum PlatformContractStatus {
    DRAFT,
    PENDING_SIGN,
    COMPLETED,
    ARCHIVED,
    VOIDED
}
