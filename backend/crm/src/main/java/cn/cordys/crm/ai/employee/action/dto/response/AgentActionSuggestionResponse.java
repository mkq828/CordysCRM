package cn.cordys.crm.ai.employee.action.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 智慧工作台「AI 跟进建议」卡片列表项（投影自 ai_employee_action 的跟进类待确认动作）。
 */
@Data
public class AgentActionSuggestionResponse {

    @Schema(description = "动作ID")
    private String id;

    @Schema(description = "主题（客户名称）")
    private String topic;

    @Schema(description = "建议摘要（跟进话术草稿）")
    private String summary;

    @Schema(description = "负责人姓名")
    private String ownerName;

    @Schema(description = "优先级（前端按阈值展示紧急/重要/建议，暂不赋值）")
    private Integer priority;
}
