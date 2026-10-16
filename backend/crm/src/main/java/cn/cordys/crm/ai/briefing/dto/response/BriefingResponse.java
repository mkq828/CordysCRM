package cn.cordys.crm.ai.briefing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 经营简报。
 */
@Data
@Schema(description = "经营简报")
public class BriefingResponse {

    @Schema(description = "简报ID")
    private String id;

    @Schema(description = "报告类型: DAILY/WEEKLY")
    private String type;

    @Schema(description = "报告周期")
    private String period;

    @Schema(description = "简报正文")
    private String content;

    @Schema(description = "生成时间(毫秒)")
    private Long createTime;
}
