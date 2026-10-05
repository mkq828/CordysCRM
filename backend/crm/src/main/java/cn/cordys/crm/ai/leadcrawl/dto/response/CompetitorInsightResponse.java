package cn.cordys.crm.ai.leadcrawl.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 竞品/行业洞察结构化结果（功能 10 第二块）。
 */
@Data
public class CompetitorInsightResponse {

    @Schema(description = "竞品概况（一句话）")
    private String overview;

    @Schema(description = "近期动态")
    private List<String> dynamics;

    @Schema(description = "行业趋势")
    private List<String> trends;

    @Schema(description = "风险提示")
    private List<String> risks;

    @Schema(description = "跟进/应对建议")
    private List<String> suggestions;

    @Schema(description = "解析失败时的原始文本兜底")
    private String rawContent;
}
