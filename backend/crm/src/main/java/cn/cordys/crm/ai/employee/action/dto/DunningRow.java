package cn.cordys.crm.ai.employee.action.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 「即将到期/已逾期」的合同或回款计划行：催款扫描的候选，供 LLM 生成催款话术时的上下文。
 * 合同与回款计划两类资源合并去重（按资源类型区分）。
 */
@Data
public class DunningRow {

    /** 资源类型：CONTRACT / PAYMENT_PLAN */
    private String resourceType;

    private String resourceId;

    private String resourceName;

    private String customerId;

    private String customerName;

    /** 资源负责人 userId（确认人 + 催款站内信接收人） */
    private String owner;

    /** 到期时间（毫秒） */
    private Long dueTime;

    /** 金额（合同金额或计划回款金额，供话术引用，不用于计算） */
    private BigDecimal amount;
}
