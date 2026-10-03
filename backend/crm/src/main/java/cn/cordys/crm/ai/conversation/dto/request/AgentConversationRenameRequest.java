package cn.cordys.crm.ai.conversation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 会话重命名请求。
 */
@Data
public class AgentConversationRenameRequest {

    @Schema(description = "新标题")
    private String title;
}
