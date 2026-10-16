package cn.cordys.crm.ai.employee.action.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 待确认动作列表项（「AI 员工-待确认」tab 展示）。
 */
@Data
public class AiEmployeeActionResponse {

    @Schema(description = "动作ID")
    private String id;

    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "客户负责人ID")
    private String ownerId;

    @Schema(description = "客户负责人姓名")
    private String ownerName;

    @Schema(description = "跟进话术草稿")
    private String content;

    @Schema(description = "建议跟进方式（电话/到访）")
    private String followMethod;

    @Schema(description = "补充建议")
    private String suggestion;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间（毫秒）")
    private Long createTime;
}
