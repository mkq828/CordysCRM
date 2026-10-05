package cn.cordys.crm.ai.leadcrawl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 竞品/行业洞察请求（纯 LLM 分析，走任务模型）。
 */
@Data
public class CompetitorInsightRequest {

    @Schema(description = "竞品名称（多个用顿号/逗号分隔）")
    private String competitor;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "关注点/诉求（可选）")
    private String focus;

    @Schema(description = "会话ID（续接历史会话时传）")
    private String conversationId;
}
