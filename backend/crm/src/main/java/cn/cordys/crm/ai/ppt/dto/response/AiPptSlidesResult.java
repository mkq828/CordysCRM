package cn.cordys.crm.ai.ppt.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * PPT 逐页结果。模型返回 JSON 解析得到；解析失败时仅回填 {@link #rawContent} 原文。
 */
@Data
public class AiPptSlidesResult {

    @Schema(description = "方案标题")
    private String title;

    @Schema(description = "逐页内容（首末页为封面/致谢）")
    private List<PptSlide> slides;

    @Schema(description = "结构化解析失败时的原文回退")
    private String rawContent;

    /**
     * 单页 PPT：标题 + 要点（3-5 条短句）+ 讲解词（notes）。
     */
    @Data
    public static class PptSlide {

        @Schema(description = "页标题")
        private String title;

        @Schema(description = "要点列表")
        private List<String> bullets;

        @Schema(description = "讲解词（演讲者备注）")
        private String notes;
    }
}
