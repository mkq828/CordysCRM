package cn.cordys.crm.ai.leadcrawl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 潜客挖掘搜索请求：行业 / 地域 / 关键词三者至少填一个，组合成联网搜索词。
 */
@Data
public class LeadMiningSearchRequest {

    @Schema(description = "行业，如「SaaS」「智能制造」")
    private String industry;

    @Schema(description = "地域，如「广州」「长三角」")
    private String region;

    @Schema(description = "关键词，如「AI 客服」")
    private String keyword;
}
