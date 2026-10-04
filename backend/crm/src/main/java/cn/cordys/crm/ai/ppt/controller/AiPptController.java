package cn.cordys.crm.ai.ppt.controller;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.controller.BaseAiStreamController;
import cn.cordys.crm.ai.controller.SseEventWriter;
import cn.cordys.crm.ai.conversation.service.AgentConversationService;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.ppt.dto.request.AiPptOutlineRequest;
import cn.cordys.crm.ai.ppt.dto.request.AiPptSlidesRequest;
import cn.cordys.crm.ai.ppt.service.AiPptService;
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
import java.util.function.Consumer;

/**
 * AI 方案/PPT 接口（登录即可用，额度框架门控）。
 * 两步式：/outline/stream 生成方案大纲；/slides/stream 由大纲生成 PPT 逐页。
 */
@RestController
@RequestMapping("/agent/ppt")
@Tag(name = "AI 方案/PPT")
public class AiPptController extends BaseAiStreamController {

    @Resource
    private AiPptService aiPptService;
    @Resource
    private AgentConversationService agentConversationService;

    @PostMapping(value = "/outline/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式生成方案大纲（先输出引导语，再回传 6 区块大纲）")
    public void outlineStream(@RequestBody AiPptOutlineRequest request, HttpServletResponse response) throws IOException {
        String orgId = OrganizationContext.getOrganizationId();
        String userId = SessionUtils.getUserId();
        doStream(response, orgId, userId, AiQuotaConstant.AI_PPT, request.getConversationId(),
                title(request.getProduct(), "方案生成"), outlineUserContent(request),
                onChunk -> aiPptService.generateOutline(orgId, request, onChunk));
    }

    @PostMapping(value = "/slides/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式生成 PPT 逐页（先输出引导语，再回传逐页内容）")
    public void slidesStream(@RequestBody AiPptSlidesRequest request, HttpServletResponse response) throws IOException {
        String orgId = OrganizationContext.getOrganizationId();
        String userId = SessionUtils.getUserId();
        doStream(response, orgId, userId, AiQuotaConstant.AI_PPT, request.getConversationId(),
                title(request.getTitle(), "PPT 逐页"), slidesUserContent(request),
                onChunk -> aiPptService.generateSlides(orgId, request, onChunk));
    }

    /** 共用 SSE 编排：建会话 → 写 run → 流式 chunk → 落库 → 写 done/error。 */
    private void doStream(HttpServletResponse response, String orgId, String userId, String featureCode,
            String conversationId, String conversationTitle, String userContent, GenerateCall generate) throws IOException {
        PrintWriter writer = beginSse(response);
        String runId = IDGenerator.nextStr();
        String assistantMessageId = IDGenerator.nextStr();
        String resolved = agentConversationService.resolveConversation(orgId, userId, featureCode,
                conversationId, conversationTitle);
        agentConversationService.saveUserMessage(resolved, runId, orgId, userId, userContent, null);

        SseEventWriter.write(writer, "run", null, runData(resolved, runId, assistantMessageId));

        StringBuilder narrative = new StringBuilder();
        try {
            AiStreamResult<?> streamResult = generate.apply(chunk -> {
                narrative.append(chunk);
                try {
                    SseEventWriter.write(writer, "chunk", resolved, chunk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            LlmUsage usage = streamResult.usage();
            Object result = streamResult.result();
            agentConversationService.saveAssistantMessage(resolved, runId, assistantMessageId, orgId, userId,
                    narrative.toString(), JSON.toJSONString(result), "done", usage);
            SseEventWriter.write(writer, "done", null, doneData(resolved, runId, assistantMessageId, usage, result));
        } catch (Exception e) {
            String message = e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage();
            agentConversationService.saveAssistantMessage(resolved, runId, assistantMessageId, orgId, userId,
                    message, null, "error", null);
            SseEventWriter.write(writer, "error", null, Map.of("message", message));
        } finally {
            writer.flush();
            writer.close();
        }
    }

    private String outlineUserContent(AiPptOutlineRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("行业：").append(request.getIndustry() == null ? "" : request.getIndustry().trim());
        sb.append("；产品/服务：").append(request.getProduct() == null ? "" : request.getProduct().trim());
        sb.append("；客户背景：").append(request.getBackground() == null ? "" : request.getBackground().trim());
        return sb.toString();
    }

    private String slidesUserContent(AiPptSlidesRequest request) {
        return "生成 PPT：" + (request.getTitle() == null ? "" : request.getTitle().trim());
    }

    @FunctionalInterface
    private interface GenerateCall {
        AiStreamResult<?> apply(Consumer<String> onChunk);
    }
}
