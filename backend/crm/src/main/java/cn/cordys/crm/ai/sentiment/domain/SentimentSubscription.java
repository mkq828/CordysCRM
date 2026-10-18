package cn.cordys.crm.ai.sentiment.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 舆情监控订阅：租户内用户维护关注的监控对象，供定时 + 手动联网搜索负面舆情/风险信号。
 */
@Data
@Table(name = "ai_sentiment_subscription")
public class SentimentSubscription extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "订阅人用户ID")
    private String userId;

    @Schema(description = "监控对象(品牌/客户/供应商名)")
    private String target;

    @Schema(description = "补充关键词")
    private String keyword;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
