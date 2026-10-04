package cn.cordys.crm.ai.employee.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 员工「虚拟会议室」会议纪要（功能 6，租户级）：董事长主持、多岗位 AI 依次发言后，用任务模型自动总结，
 * 按 DeepSeek 参考页结构沉淀标题、时间、主持人、记录人、参会人、发言纪要、后续行动与任务清单。
 */
@Data
@Table(name = "ai_meeting_minutes")
public class AiMeetingMinutes extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

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

    @Schema(description = "参会人名单(JSON 数组)")
    private String attendeesJson;

    @Schema(description = "发言纪要(JSON 数组)")
    private String contentJson;

    @Schema(description = "后续行动(JSON 数组)")
    private String nextJson;

    @Schema(description = "任务清单(JSON 数组[任务/截止/交付物])")
    private String tasksJson;
}
