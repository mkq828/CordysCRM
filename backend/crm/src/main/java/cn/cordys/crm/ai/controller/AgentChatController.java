package cn.cordys.crm.ai.controller;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.conversation.service.AgentConversationService;
import cn.cordys.crm.ai.dto.request.AiChatStreamRequest;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.service.AgentChatService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * AI 对话接口（登录即可用）。SSE 事件协议与前端 ai-chat 对齐：run / chunk / done / error。
 * 会话与消息通过 AgentConversationService 落库，featureCode=chat，承载 AI 中心左侧历史。
 */
@RestController
@RequestMapping("/agent")
@Tag(name = "AI 对话")
public class AgentChatController extends BaseAiStreamController {

    @Resource
    private AgentChatService agentChatService;
    @Resource
    private AgentConversationService agentConversationService;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 对话流式响应")
    public void chatStream(@RequestBody AiChatStreamRequest request, HttpServletResponse response) throws IOException {
        String organizationId = OrganizationContext.getOrganizationId();
        String userId = SessionUtils.getUserId();
        PrintWriter writer = beginSse(response);

        String runId = IDGenerator.nextStr();
        String assistantMessageId = IDGenerator.nextStr();
        String conversationId = agentConversationService.resolveConversation(organizationId, userId,
                AgentConversationService.FEATURE_CHAT, request.getConversationId(), title(request.getMessage(), "哆咪AI对话"));
        agentConversationService.saveUserMessage(conversationId, runId, organizationId, userId, request.getMessage(), null);

        SseEventWriter.write(writer, "run", null, runData(conversationId, runId, assistantMessageId));

        StringBuilder answer = new StringBuilder();
        try {
            LlmUsage usage = agentChatService.chat(organizationId, request, chunk -> {
                answer.append(chunk);
                try {
                    SseEventWriter.write(writer, "chunk", conversationId, chunk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, organizationId, userId,
                    answer.toString(), null, "done", usage);
            SseEventWriter.write(writer, "done", null, doneData(conversationId, runId, assistantMessageId, usage, null));
        } catch (Exception e) {
            String message = e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage();
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, organizationId, userId,
                    message, null, "error", null);
            SseEventWriter.write(writer, "error", null, Map.of("message", message));
        } finally {
            writer.flush();
            writer.close();
        }
    }
}
