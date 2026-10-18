package cn.cordys.crm.ai.tender.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 标讯订阅：租户内用户维护的招标订阅条件，供定时 + 手动联网搜索招标信息。
 */
@Data
@Table(name = "ai_tender_subscription")
public class TenderSubscription extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "订阅人用户ID")
    private String userId;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "地域")
    private String region;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
