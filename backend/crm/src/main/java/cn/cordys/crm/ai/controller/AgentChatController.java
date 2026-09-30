package cn.cordys.crm.ai.controller;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.dto.request.AiChatStreamRequest;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.service.AgentChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 对话接口（登录即可用）。SSE 事件协议与前端 ai-chat 对齐：run / chunk / done / error。
 */
@RestController
@RequestMapping("/agent")
@Tag(name = "AI 对话")
public class AgentChatController {

    @Resource
    private AgentChatService agentChatService;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 对话流式响应")
    public void chatStream(@RequestBody AiChatStreamRequest request, HttpServletResponse response) throws IOException {
        String organizationId = OrganizationContext.getOrganizationId();

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        PrintWriter writer = response.getWriter();

        String conversationId = StringUtils.isBlank(request.getConversationId())
                ? IDGenerator.nextStr()
                : request.getConversationId();
        String runId = IDGenerator.nextStr();
        String assistantMessageId = IDGenerator.nextStr();

        Map<String, Object> runData = new LinkedHashMap<>();
        runData.put("conversationId", conversationId);
        runData.put("runId", runId);
        runData.put("assistantMessageId", assistantMessageId);
        writeEvent(writer, "run", null, runData);

        try {
            LlmUsage usage = agentChatService.chat(organizationId, request, chunk -> {
                try {
                    writeEvent(writer, "chunk", conversationId, chunk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Map<String, Object> doneData = new LinkedHashMap<>();
            doneData.put("runId", runId);
            doneData.put("conversationId", conversationId);
            doneData.put("assistantMessageId", assistantMessageId);
            doneData.put("totalTokens", usage.getTotalTokens());
            doneData.put("input", usage.getInputTokens());
            doneData.put("output", usage.getOutputTokens());
            writeEvent(writer, "done", null, doneData);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage();
            writeEvent(writer, "error", null, Map.of("message", message));
        } finally {
            writer.flush();
            writer.close();
        }
    }

    /**
     * 写一个 SSE 事件帧。data 为 String 时按纯文本逐行输出（兼容多行 markdown），否则 JSON 序列化。
     */
    private void writeEvent(PrintWriter writer, String event, String id, Object data) throws IOException {
        writer.write("event: " + event + "\n");
        if (id != null) {
            writer.write("id: " + id + "\n");
        }
        String content = data instanceof String s ? s : JSON.toJSONString(data);
        for (String line : content.replace("\r\n", "\n").split("\n", -1)) {
            writer.write("data: " + line + "\n");
        }
        writer.write("\n");
        writer.flush();
    }
}
