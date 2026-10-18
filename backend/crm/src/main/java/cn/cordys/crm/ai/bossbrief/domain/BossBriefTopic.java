package cn.cordys.crm.ai.bossbrief.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 老板晨报关注主题：租户内用户维护的关注主题（行业/市场/政策等），供定时 + 手动联网搜索当日要闻。
 */
@Data
@Table(name = "ai_boss_brief_topic")
public class BossBriefTopic extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "订阅人用户ID")
    private String userId;

    @Schema(description = "关注主题(行业/市场/政策等)")
    private String topic;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
