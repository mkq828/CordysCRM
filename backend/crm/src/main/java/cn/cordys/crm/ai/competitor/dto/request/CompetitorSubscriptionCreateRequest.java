package cn.cordys.crm.ai.competitor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 新建竞品动态订阅请求。
 */
@Data
public class CompetitorSubscriptionCreateRequest {

    @Schema(description = "竞品名称，如「Salesforce」")
    private String competitor;

    @Schema(description = "补充关键词，如「CRM 融资」")
    private String keyword;
}
