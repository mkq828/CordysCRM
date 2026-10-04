package cn.cordys.crm.ai.employee.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 员工岗位 Agent 新增/更新请求。
 */
@Data
public class AiEmployeeAgentSaveRequest {

    @Schema(description = "岗位 Agent ID（更新时必填）")
    private String id;

    @Schema(description = "岗位 Agent 名称")
    private String name;

    @Schema(description = "关联员工角色(ai_employee.id)")
    private String employeeId;

    @Schema(description = "追加的自定义提示词(可选)")
    private String customPrompt;

    @Schema(description = "是否挂知识库检索(1=是)")
    private Boolean knowledgeEnabled;

    @Schema(description = "排序")
    private Long sort;
}
