package cn.cordys.crm.ai.competitor.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 竞品动态订阅：租户内用户维护关注的竞品，供定时 + 手动联网搜索竞品最新动态。
 */
@Data
@Table(name = "ai_competitor_subscription")
public class CompetitorSubscription extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "订阅人用户ID")
    private String userId;

    @Schema(description = "竞品名称")
    private String competitor;

    @Schema(description = "补充关键词")
    private String keyword;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
