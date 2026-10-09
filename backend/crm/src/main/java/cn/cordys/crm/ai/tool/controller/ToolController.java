package cn.cordys.crm.ai.tool.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.controller.BaseAiStreamController;
import cn.cordys.crm.ai.controller.SseEventWriter;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.tool.dto.request.AiToolImageRequest;
import cn.cordys.crm.ai.tool.dto.request.AiToolTaskPageRequest;
import cn.cordys.crm.ai.tool.dto.request.AiToolTextRequest;
import cn.cordys.crm.ai.tool.dto.request.AiToolTranscodeRequest;
import cn.cordys.crm.ai.tool.dto.response.AiToolTaskResponse;
import cn.cordys.crm.ai.tool.service.AiToolService;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 工具中心接口（创作中心子模块，专业版+ G3 门控，文生图/文案工具走 G2 额度）。
 * 文案类工具走流式 SSE；文生图 / 视频转格式走异步任务，前端轮询 /tasks 感知进度。
 */
@RestController
@RequestMapping("/agent/tool")
@Tag(name = "AI 工具中心")
public class ToolController extends BaseAiStreamController {

    @Resource
    private AiToolService aiToolService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通工具中心")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_TOOL);
    }

    @PostMapping(value = "/text/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "文案类工具流式生成（文案改写/爆款标题/口播脚本）")
    public void textStream(@Valid @RequestBody AiToolTextRequest request, HttpServletResponse response) throws IOException {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        PrintWriter writer = beginSse(response);
        try {
            AiStreamResult<String> result = aiToolService.generateText(orgId, request, chunk -> {
                try {
                    SseEventWriter.write(writer, "chunk", null, chunk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("content", result.result());
            payload.put("input", result.usage().getInputTokens());
            payload.put("output", result.usage().getOutputTokens());
            payload.put("totalTokens", result.usage().getTotalTokens());
            SseEventWriter.write(writer, "done", null, payload);
        } catch (Exception e) {
            SseEventWriter.write(writer, "error", null, Map.of("message",
                    e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage()));
        } finally {
            writer.flush();
            writer.close();
        }
    }

    @PostMapping("/image")
    @Operation(summary = "提交文生图任务（异步）")
    public AiToolTaskResponse createImage(@Valid @RequestBody AiToolImageRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        AiToolTaskResponse response = aiToolService.submitImage(request, orgId, userId);
        aiToolService.generateImageAsync(response.getId(), orgId, userId);
        return response;
    }

    @PostMapping("/transcode")
    @Operation(summary = "提交视频转格式任务（异步）")
    public AiToolTaskResponse createTranscode(@Valid @RequestBody AiToolTranscodeRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        AiToolTaskResponse response = aiToolService.submitTranscode(request, orgId, userId);
        aiToolService.generateTranscodeAsync(response.getId(), orgId, userId);
        return response;
    }

    @GetMapping("/tasks")
    @Operation(summary = "任务列表（轮询进度）")
    public List<AiToolTaskResponse> list() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return aiToolService.list(orgId);
    }

    @PostMapping("/tasks/page")
    @Operation(summary = "任务分页列表（生成记录）")
    public Pager<List<AiToolTaskResponse>> page(@Valid @RequestBody AiToolTaskPageRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return aiToolService.page(orgId, request);
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "任务详情")
    public AiToolTaskResponse get(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return aiToolService.get(orgId, id);
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "删除任务（同时清理附件）")
    public void delete(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        aiToolService.delete(orgId, id);
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_TOOL)) {
            throw new GenericException("当前版本未开通工具中心，请升级至专业版");
        }
    }
}
