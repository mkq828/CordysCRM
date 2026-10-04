package cn.cordys.crm.ai.employee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 员工虚拟会议室会议纪要（列表项/详情）。
 */
@Data
public class AiMeetingMinutesResponse {

    @Schema(description = "纪要 ID")
    private String id;

    @Schema(description = "会议标题")
    private String title;

    @Schema(description = "会议时间文本")
    private String timeText;

    @Schema(description = "会议地点")
    private String place;

    @Schema(description = "主持人(董事长)")
    private String host;

    @Schema(description = "记录人(董事会秘书)")
    private String recorder;

    @Schema(description = "参会人名单")
    private List<String> attendees;

    @Schema(description = "发言纪要")
    private List<String> content;

    @Schema(description = "后续行动")
    private List<String> next;

    @Schema(description = "任务清单")
    private List<Task> tasks;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;

    @Data
    public static class Task {

        @Schema(description = "任务描述")
        private String task;

        @Schema(description = "截止日期")
        private String deadline;

        @Schema(description = "交付物")
        private String deliverable;
    }
}
