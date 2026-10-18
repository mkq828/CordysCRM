package cn.cordys.crm.ai.bossbrief.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 新建老板晨报关注主题请求。
 */
@Data
public class BossBriefTopicCreateRequest {

    @Schema(description = "关注主题(行业/市场/政策等)，如「人工智能行业」")
    private String topic;
}
