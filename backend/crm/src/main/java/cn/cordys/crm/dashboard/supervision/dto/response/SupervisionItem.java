package cn.cordys.crm.dashboard.supervision.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 督办任务条目。
 */
@Data
@Schema(description = "督办任务条目")
public class SupervisionItem {

    @Schema(description = "任务ID")
    private String id;

    @Schema(description = "被督办人 userId")
    private String assignee;

    @Schema(description = "被督办人姓名")
    private String assigneeName;

    @Schema(description = "督办内容")
    private String content;

    @Schema(description = "截止时间(毫秒)")
    private Long deadline;

    @Schema(description = "状态: PENDING/DONE/OVERDUE")
    private String status;

    @Schema(description = "下达人 userId")
    private String createUser;

    @Schema(description = "下达时间(毫秒)")
    private Long createTime;
}
