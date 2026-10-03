package cn.cordys.crm.ai.content.controller;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.content.dto.request.AiContentGenerateRequest;
import cn.cordys.crm.ai.content.dto.response.AiContentGenerateResponse;
import cn.cordys.crm.ai.content.service.AiContentGenerateService;
import cn.cordys.crm.ai.controller.BaseAiStreamController;
import cn.cordys.crm.ai.controller.SseEventWriter;
import cn.cordys.crm.ai.conversation.service.AgentConversationService;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.llm.LlmUsage;
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
 * AI 获客内容接口（登录即可用，额度框架门控）。
 * /generate 同步兼容；/generate/stream 为流式 + 会话记录主路径（先流式生成说明，再回传物料包）。
 */
@RestController
@RequestMapping("/agent/content")
@Tag(name = "AI 获客内容")
public class AiContentController extends BaseAiStreamController {

    @Resource
    private AiContentGenerateService aiContentGenerateService;
    @Resource
    private AgentConversationService agentConversationService;

    @PostMapping("/generate")
    @Operation(summary = "生成获客内容（选题/标题/正文/配图文案/封面文案/话题标签/发布时间/口播脚本）")
    public AiContentGenerateResponse generate(@RequestBody AiContentGenerateRequest request) {
        return aiContentGenerateService.generate(OrganizationContext.getOrganizationId(), request);
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式生成获客内容（先输出生成说明，再回传物料包）")
    public void generateStream(@RequestBody AiContentGenerateRequest request, HttpServletResponse response) throws IOException {
        String orgId = OrganizationContext.getOrganizationId();
        String userId = SessionUtils.getUserId();
        PrintWriter writer = beginSse(response);

        String runId = IDGenerator.nextStr();
        String assistantMessageId = IDGenerator.nextStr();
        String conversationId = agentConversationService.resolveConversation(orgId, userId, AiQuotaConstant.AI_ACQUIRE,
                request.getConversationId(), title(request.getProduct(), "获客内容生成"));
        agentConversationService.saveUserMessage(conversationId, runId, orgId, userId, userContent(request), null);

        SseEventWriter.write(writer, "run", null, runData(conversationId, runId, assistantMessageId));

        StringBuilder narrative = new StringBuilder();
        try {
            AiStreamResult<AiContentGenerateResponse> streamResult = aiContentGenerateService.generateStream(orgId, request, chunk -> {
                narrative.append(chunk);
                try {
                    SseEventWriter.write(writer, "chunk", conversationId, chunk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            LlmUsage usage = streamResult.usage();
            AiContentGenerateResponse result = streamResult.result();
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

    private String userContent(AiContentGenerateRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("行业：").append(request.getIndustry() == null ? "" : request.getIndustry().trim());
        sb.append("；产品/卖点：").append(request.getProduct() == null ? "" : request.getProduct().trim());
        return sb.toString();
    }
}
