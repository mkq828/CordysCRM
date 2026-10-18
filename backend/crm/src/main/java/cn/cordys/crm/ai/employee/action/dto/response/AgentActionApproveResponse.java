package cn.cordys.crm.ai.employee.action.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 智慧工作台「AI 方案审核」卡片列表项（投影自 ai_employee_action 的非跟进类待确认动作）。
 */
@Data
public class AgentActionApproveResponse {

    @Schema(description = "动作ID")
    private String id;

    @Schema(description = "主题（客户/资源名称）")
    private String topic;

    @Schema(description = "方案摘要")
    private String summary;

    @Schema(description = "负责人姓名")
    private String ownerName;

    @Schema(description = "动作类型（DUNNING/TAG/CLUE_FLOW/FILL_FIELD，前端映射文案）")
    private String type;
}
