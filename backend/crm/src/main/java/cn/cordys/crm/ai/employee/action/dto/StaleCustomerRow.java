package cn.cordys.crm.ai.employee.action.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 「久未联系」客户行：扫描查询结果，供 LLM 生成跟进草稿时的上下文。
 */
@Data
public class StaleCustomerRow {

    private String customerId;

    private String customerName;

    /** 客户负责人 userId（确认人） */
    private String owner;

    /** 最新跟进时间（毫秒，可为空表示从未跟进） */
    private Long followTime;

    /** 累计合同额（未签约客户为 0，用于重要度排序） */
    private BigDecimal contractAmount;
}
