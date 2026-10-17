package cn.cordys.crm.ai.employee.action.dto;

import lombok.Data;

/**
 * 「已填写的必填文本字段」行：扫描服务用它反推每个候选客户缺失了哪些必填字段。
 */
@Data
public class FillFieldValueRow {

    private String customerId;

    private String fieldId;
}
