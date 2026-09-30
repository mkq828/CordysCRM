package cn.cordys.crm.ai.dto.request;

import lombok.Data;

import java.util.List;

/**
 * AI 对话流式请求（与前端 AgentChatStreamParams 对齐）。
 */
@Data
public class AiChatStreamRequest {

    private String message;

    private String conversationId;

    /** 请求级幂等键，未产生 runId 前用于取消定位 */
    private String requestId;

    private List<String> mcpIds;

    private List<String> attachmentIds;

    private List<String> picIds;
}
