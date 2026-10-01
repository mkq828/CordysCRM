package cn.cordys.crm.system.constants;

import lombok.Getter;

/**
 * 租户续费/升级申请状态
 */
@Getter
public enum TenantPlanApplicationStatus {

    /**
     * 待核销（已提交，等 admin 确认收款）
     */
    PENDING("PENDING"),

    /**
     * 已核销（已开通/升级套餐）
     */
    APPROVED("APPROVED"),

    /**
     * 已驳回
     */
    CANCELLED("CANCELLED");

    private final String value;

    TenantPlanApplicationStatus(String value) {
        this.value = value;
    }
}
