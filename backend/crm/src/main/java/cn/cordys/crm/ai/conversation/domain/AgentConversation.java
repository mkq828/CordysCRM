package cn.cordys.crm.ai.conversation.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 会话（agent_conversation 表）。同一套表承载四种能力（chat/军师/问答/获客），
 * 用 {@link #featureCode} 区分；会话按用户隔离，用于左侧历史记录与消息回放。
 */
@Data
@Table(name = "agent_conversation")
public class AgentConversation extends BaseModel {

    @Schema(description = "对话标题（首条消息前若干字）")
    private String title;

    @Schema(description = "能力类型：chat / ai_advisor / ai_kb / ai_acquire")
    private String featureCode;

    @Schema(description = "用户ID（会话归属，个人历史）")
    private String userId;

    @Schema(description = "组织ID")
    private String organizationId;
}
