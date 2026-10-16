package cn.cordys.crm.ai.briefing.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 经营大屏「晨报/周报」：定时或手动生成的经营简报，落库供首屏「AI 经营简报」卡读取。
 */
@Data
@Table(name = "sys_ai_briefing")
public class AiBriefing extends BaseModel {

    public static final String TYPE_DAILY = "DAILY";
    public static final String TYPE_WEEKLY = "WEEKLY";

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "报告类型: DAILY/WEEKLY")
    private String type;

    @Schema(description = "报告周期(yyyy-MM-dd / 周一日期)")
    private String period;

    @Schema(description = "简报正文")
    private String content;
}
