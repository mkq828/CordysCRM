package cn.cordys.crm.ai.tender.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 新建标讯订阅请求。
 */
@Data
public class TenderSubscriptionCreateRequest {

    @Schema(description = "行业，如「SaaS」「智能制造」")
    private String industry;

    @Schema(description = "地域，如「广州」「长三角」")
    private String region;

    @Schema(description = "关键词，如「CRM 采购」")
    private String keyword;
}
