package cn.cordys.crm.ai.conversation.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 会话分页请求。keyword 继承自 BaseCondition 匹配标题，featureCode 区分能力类型。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentConversationPageRequest extends BasePageRequest {

    @Schema(description = "能力类型：chat / ai_advisor / ai_kb / ai_acquire")
    private String featureCode;
}
