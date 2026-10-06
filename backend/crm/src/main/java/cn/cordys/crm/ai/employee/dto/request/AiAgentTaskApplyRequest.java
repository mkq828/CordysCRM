package cn.cordys.crm.ai.employee.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 确认执行 AI 员工提出的客户字段修改。仅回传要改的字段，旧值由服务端重新读取，不信任前端回显。
 */
@Data
public class AiAgentTaskApplyRequest {

    private String customerId;

    private List<Change> changes;

    @Data
    public static class Change {

        /** 字段 id；内置客户名称用 "name" */
        private String fieldId;

        private String newValue;
    }
}
