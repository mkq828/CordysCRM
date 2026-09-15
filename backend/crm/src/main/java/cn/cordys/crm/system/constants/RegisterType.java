package cn.cordys.crm.system.constants;

import lombok.Getter;

/**
 * 注册类型枚举
 */
@Getter
public enum RegisterType {

    /**
     * 个人注册
     */
    PERSONAL("PERSONAL"),

    /**
     * 企业注册
     */
    ENTERPRISE("ENTERPRISE");

    private final String value;

    RegisterType(String value) {
        this.value = value;
    }

    /**
     * 判断是否为合法注册类型
     */
    public static boolean isValid(String type) {
        return PERSONAL.value.equals(type) || ENTERPRISE.value.equals(type);
    }

    /**
     * 判断是否为企业注册
     */
    public static boolean isEnterprise(String type) {
        return ENTERPRISE.value.equals(type);
    }
}
