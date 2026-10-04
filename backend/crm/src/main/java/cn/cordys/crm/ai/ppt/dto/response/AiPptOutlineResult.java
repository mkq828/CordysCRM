package cn.cordys.crm.ai.ppt.dto.response;

import cn.cordys.crm.ai.ppt.dto.PptSection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 方案大纲结果。模型返回 JSON 解析得到；解析失败时仅回填 {@link #rawContent} 原文。
 */
@Data
public class AiPptOutlineResult {

    @Schema(description = "方案标题")
    private String title;

    @Schema(description = "方案大纲区块（背景/方案/架构/实施/报价/风险）")
    private List<PptSection> sections;

    @Schema(description = "结构化解析失败时的原文回退")
    private String rawContent;
}
