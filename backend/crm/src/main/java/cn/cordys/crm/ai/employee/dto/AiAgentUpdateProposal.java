package cn.cordys.crm.ai.employee.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 员工提出的「客户字段修改建议」。由写建议工具生成、经 {@code done.payload} 下发给前端，让用户确认后再真正落库。
 */
@Data
public class AiAgentUpdateProposal {

    /** 提案类型，前端据此渲染确认卡 */
    private String type = "customer_update";

    private String customerId;

    private String customerName;

    private List<Change> changes;

    /** 单字段改动：旧值 → 新值 */
    @Data
    public static class Change {

        /** 字段 id；内置客户名称用 "name" */
        private String fieldId;

        private String fieldLabel;

        private String oldValue;

        private String newValue;
    }
}
