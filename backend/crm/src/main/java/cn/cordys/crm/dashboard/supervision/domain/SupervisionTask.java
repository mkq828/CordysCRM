package cn.cordys.crm.dashboard.supervision.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 经营大屏「督办闭环」任务：老板下达 → 被督办人收到站内信 → 到期自动标记逾期。
 */
@Data
@Table(name = "sys_supervision_task")
public class SupervisionTask extends BaseModel {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_OVERDUE = "OVERDUE";

    @Schema(description = "租户组织ID")
    private String organizationId;

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
}
