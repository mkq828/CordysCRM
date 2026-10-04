package cn.cordys.crm.ai.ppt.dto.request;

import cn.cordys.crm.ai.ppt.dto.PptSection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 方案/PPT — 生成 PPT 逐页请求：前端把（可编辑后的）大纲区块原样传回。
 */
@Data
public class AiPptSlidesRequest {

    @Schema(description = "方案标题")
    private String title;

    @Schema(description = "方案大纲区块（可编辑后回传）")
    private List<PptSection> sections;

    @Schema(description = "会话 ID（可空，为空时服务端新建会话）")
    private String conversationId;
}
