package cn.cordys.crm.ai.tender.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 标讯搜索结果：订阅搜索到的招标信息，按 URL 去重落库。
 */
@Data
@Table(name = "ai_tender_result")
public class TenderResult extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "订阅ID")
    private String subscriptionId;

    @Schema(description = "招标标题")
    private String title;

    @Schema(description = "来源链接")
    private String url;

    @Schema(description = "摘要")
    private String snippet;

    @Schema(description = "正文(可空)")
    private String content;
}
