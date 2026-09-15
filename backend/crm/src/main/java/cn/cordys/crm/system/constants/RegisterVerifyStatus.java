package cn.cordys.crm.system.constants;

import lombok.Getter;

/**
 * 注册申请审核状态枚举
 */
@Getter
public enum RegisterVerifyStatus {

    /**
     * 待审核
     */
    PENDING("PENDING"),

    /**
     * 审核通过
     */
    APPROVED("APPROVED"),

    /**
     * 审核驳回
     */
    REJECTED("REJECTED");

    private final String value;

    RegisterVerifyStatus(String value) {
        this.value = value;
    }
}
