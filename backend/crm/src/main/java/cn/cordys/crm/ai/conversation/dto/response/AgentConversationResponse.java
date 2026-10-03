package cn.cordys.crm.ai.conversation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 会话列表项（与前端 AgentConversationItem 对齐）。
 */
@Data
public class AgentConversationResponse {

    @Schema(description = "会话ID")
    private String id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "能力类型：chat / ai_advisor / ai_kb / ai_acquire")
    private String featureCode;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "更新人")
    private String updateUser;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;
}
