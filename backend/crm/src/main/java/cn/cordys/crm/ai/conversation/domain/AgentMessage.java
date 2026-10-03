package cn.cordys.crm.ai.conversation.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 消息（agent_message 表）。一条消息对应一轮问答里的用户输入或 AI 输出；
 * 结构化能力（军师/问答/获客）把结果存进 {@link #payload}，{@link #content} 保存流式正文用于列表预览与纯文本回放。
 */
@Data
@Table(name = "agent_message")
public class AgentMessage extends BaseModel {

    @Schema(description = "消息角色：USER / ASSISTANT")
    private String role;

    @Schema(description = "本轮执行ID")
    private String runId;

    @Schema(description = "所属会话ID")
    private String conversationId;

    @Schema(description = "输入 token 数")
    private Long inputTokens;

    @Schema(description = "输出 token 数")
    private Long outputTokens;

    @Schema(description = "总 token 数")
    private Long totalTokens;

    @Schema(description = "消息正文（流式文本/markdown）")
    private String content;

    @Schema(description = "能力结构化结果 JSON")
    private String payload;

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "点赞/点踩：true 点赞，false 点踩，null 未评价")
    private Boolean helpful;

    @Schema(description = "消息状态：done 已完成，stopped 已停止")
    private String status;
}
