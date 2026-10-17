package cn.cordys.crm.ai.employee.action.dto;

import lombok.Data;

/**
 * 「必填文本字段缺失」的客户行：补全数据扫描的候选。缺失哪些字段由扫描服务在 Java 侧按已填字段反推。
 */
@Data
public class FillFieldCustomerRow {

    private String customerId;

    private String customerName;

    /** 客户负责人 userId（确认人） */
    private String owner;
}
