package cn.cordys.crm.ai.ppt.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 方案/PPT — 生成方案大纲请求：客户背景 → 结构化解决方案大纲。
 */
@Data
public class AiPptOutlineRequest {

    @Schema(description = "客户名称（可选）")
    private String customerName;

    @Schema(description = "行业，必填")
    private String industry;

    @Schema(description = "产品 / 服务（我方提供的），必填")
    private String product;

    @Schema(description = "客户背景与痛点，必填")
    private String background;

    @Schema(description = "预算（可选）")
    private String budget;

    @Schema(description = "竞品（可选）")
    private String competitor;

    @Schema(description = "方案侧重（可选，如「强调性价比」「强调实施速度」）")
    private String focus;

    @Schema(description = "会话 ID（可空，为空时服务端新建会话）")
    private String conversationId;
}
