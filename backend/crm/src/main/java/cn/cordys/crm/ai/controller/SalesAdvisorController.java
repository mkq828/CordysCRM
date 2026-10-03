package cn.cordys.crm.ai.controller;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.conversation.service.AgentConversationService;
import cn.cordys.crm.ai.dto.request.SalesAdvisorAnalyzeRequest;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.dto.response.SalesAdvisorAnalyzeResponse;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.service.SalesAdvisorService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * AI 销售会话军师接口（登录即可用，额度框架门控）。
 * /analyze 同步兼容；/analyze/stream 为流式 + 会话记录主路径（先流式分析结论，再回传结构化结果）。
 */
@RestController
@RequestMapping("/agent/advisor")
@Tag(name = "AI 销售会话军师")
public class SalesAdvisorController extends BaseAiStreamController {

    @Resource
    private SalesAdvisorService salesAdvisorService;
    @Resource
    private AgentConversationService agentConversationService;

    @PostMapping("/analyze")
    @Operation(summary = "分析销售会话（粘贴文本 / 截图），返回结构化分析结果")
    public SalesAdvisorAnalyzeResponse analyze(@RequestBody SalesAdvisorAnalyzeRequest request) {
        return salesAdvisorService.analyze(OrganizationContext.getOrganizationId(), request);
    }

    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式分析销售会话（先输出分析结论，再回传结构化结果）")
    public void analyzeStream(@RequestBody SalesAdvisorAnalyzeRequest request, HttpServletResponse response) throws IOException {
        String orgId = OrganizationContext.getOrganizationId();
        String userId = SessionUtils.getUserId();
        PrintWriter writer = beginSse(response);

        String runId = IDGenerator.nextStr();
        String assistantMessageId = IDGenerator.nextStr();
        String conversationId = agentConversationService.resolveConversation(orgId, userId, AiQuotaConstant.AI_ADVISOR,
                request.getConversationId(), title(request.getMessage(), "会话军师分析"));
        agentConversationService.saveUserMessage(conversationId, runId, orgId, userId, userContent(request), null);

        SseEventWriter.write(writer, "run", null, runData(conversationId, runId, assistantMessageId));

        StringBuilder narrative = new StringBuilder();
        try {
            AiStreamResult<SalesAdvisorAnalyzeResponse> streamResult = salesAdvisorService.analyzeStream(orgId, request, chunk -> {
                narrative.append(chunk);
                try {
                    SseEventWriter.write(writer, "chunk", conversationId, chunk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            LlmUsage usage = streamResult.usage();
            SalesAdvisorAnalyzeResponse result = streamResult.result();
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, orgId, userId,
                    narrative.toString(), JSON.toJSONString(result), "done", usage);
            SseEventWriter.write(writer, "done", null, doneData(conversationId, runId, assistantMessageId, usage, result));
        } catch (Exception e) {
            String message = e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage();
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, orgId, userId,
                    message, null, "error", null);
            SseEventWriter.write(writer, "error", null, Map.of("message", message));
        } finally {
            writer.flush();
            writer.close();
        }
    }

    private String userContent(SalesAdvisorAnalyzeRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (StringUtils.isNotBlank(message)) {
            return message;
        }
        int n = request.getPicIds() == null ? 0 : request.getPicIds().size();
        return n > 0 ? "【聊天记录截图】×" + n : "";
    }
}
