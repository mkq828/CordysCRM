package cn.cordys.crm.ai.knowledge.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.controller.BaseAiStreamController;
import cn.cordys.crm.ai.controller.SseEventWriter;
import cn.cordys.crm.ai.conversation.service.AgentConversationService;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.knowledge.dto.request.AiKnowledgeAskRequest;
import cn.cordys.crm.ai.knowledge.dto.request.AiKnowledgeConfigRequest;
import cn.cordys.crm.ai.knowledge.dto.request.AiKnowledgeDocPageRequest;
import cn.cordys.crm.ai.knowledge.dto.response.AiKnowledgeAnswerResponse;
import cn.cordys.crm.ai.knowledge.dto.response.AiKnowledgeConfigResponse;
import cn.cordys.crm.ai.knowledge.dto.response.AiKnowledgeDocResponse;
import cn.cordys.crm.ai.knowledge.service.AiKnowledgeService;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * 企业知识库接口（登录即可用，问答走额度框架门控）。
 * /ask 同步兼容；/ask/stream 为流式 + 会话记录主路径（回答正文流式输出，出处服务端回填）。
 */
@RestController
@RequestMapping("/agent/kb")
@Tag(name = "企业知识库")
public class AiKnowledgeController extends BaseAiStreamController {

    @Resource
    private AiKnowledgeService aiKnowledgeService;
    @Resource
    private AgentConversationService agentConversationService;

    @PostMapping("/doc/page")
    @Operation(summary = "知识库-文档分页列表")
    public Pager<List<AiKnowledgeDocResponse>> page(@RequestBody AiKnowledgeDocPageRequest request) {
        return aiKnowledgeService.page(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/doc/upload")
    @Operation(summary = "知识库-上传并解析文档")
    public AiKnowledgeDocResponse upload(@RequestPart("file") MultipartFile file) {
        return aiKnowledgeService.upload(file, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @GetMapping("/doc/delete/{id}")
    @Operation(summary = "知识库-删除文档")
    public void delete(@PathVariable String id) {
        aiKnowledgeService.delete(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/ask")
    @Operation(summary = "知识库-检索问答")
    public AiKnowledgeAnswerResponse ask(@RequestBody AiKnowledgeAskRequest request) {
        return aiKnowledgeService.ask(request, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/config")
    @Operation(summary = "知识库-读取出处片段长度设置")
    public AiKnowledgeConfigResponse config() {
        return new AiKnowledgeConfigResponse(aiKnowledgeService.getSnippetMax());
    }

    @PostMapping("/config")
    @Operation(summary = "知识库-更新出处片段长度设置")
    public void updateConfig(@RequestBody AiKnowledgeConfigRequest request) {
        int max = request.getSnippetMax() == null ? 0 : request.getSnippetMax();
        aiKnowledgeService.updateSnippetMax(max);
    }

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "知识库-流式检索问答（回答正文流式输出，出处回填）")
    public void askStream(@RequestBody AiKnowledgeAskRequest request, HttpServletResponse response) throws IOException {
        String orgId = OrganizationContext.getOrganizationId();
        String userId = SessionUtils.getUserId();
        PrintWriter writer = beginSse(response);

        String runId = IDGenerator.nextStr();
        String assistantMessageId = IDGenerator.nextStr();
        String conversationId = agentConversationService.resolveConversation(orgId, userId, AiQuotaConstant.AI_KB,
                request.getConversationId(), title(request.getQuestion(), "知识库问答"));
        agentConversationService.saveUserMessage(conversationId, runId, orgId, userId, request.getQuestion(), null);

        SseEventWriter.write(writer, "run", null, runData(conversationId, runId, assistantMessageId));

        StringBuilder answer = new StringBuilder();
        try {
            AiStreamResult<AiKnowledgeAnswerResponse> streamResult = aiKnowledgeService.askStream(request, orgId, chunk -> {
                answer.append(chunk);
                try {
                    SseEventWriter.write(writer, "chunk", conversationId, chunk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            LlmUsage usage = streamResult.usage();
            AiKnowledgeAnswerResponse result = streamResult.result();
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, orgId, userId,
                    answer.toString(), JSON.toJSONString(result), "done", usage);
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
}
