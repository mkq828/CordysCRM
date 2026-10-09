package cn.cordys.crm.ai.bi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 问数 BI（功能 12）目录：可查指标、示例问题、可查范围提示（供前端引导）。
 */
@Data
public class BiCatalogResponse {

    @Schema(description = "可查指标（key + 中文名）")
    private List<CatalogItem> metrics;

    @Schema(description = "示例问题（输入框下方 chips）")
    private List<String> sampleQuestions;

    @Schema(description = "可查范围提示（橙色小字）")
    private String scopeHint;

    public record CatalogItem(String key, String name) {
    }
}
