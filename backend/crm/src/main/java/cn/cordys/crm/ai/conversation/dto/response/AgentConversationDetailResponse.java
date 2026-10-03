package cn.cordys.crm.ai.conversation.dto.response;

import cn.cordys.crm.ai.conversation.domain.AgentMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 会话详情：会话元信息 + 按时间升序的消息列表（与前端 AgentConversationDetail 对齐）。
 */
@Data
public class AgentConversationDetailResponse {

    @Schema(description = "会话信息")
    private AgentConversationResponse conversation;

    @Schema(description = "消息列表（按创建时间升序）")
    private List<AgentMessage> messages;
}
