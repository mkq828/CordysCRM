package cn.cordys.crm.system.constants;

import lombok.Getter;

/**
 * 租户套餐状态
 */
@Getter
public enum TenantPlanStatus {

    /**
     * 试用中
     */
    FREE("FREE"),

    /**
     * 已开通（已付费）
     */
    ACTIVE("ACTIVE"),

    /**
     * 已到期
     */
    EXPIRED("EXPIRED");

    private final String value;

    TenantPlanStatus(String value) {
        this.value = value;
    }
}
