package cn.cordys.crm.ai.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.request.SalesAdvisorAnalyzeRequest;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.dto.response.SalesAdvisorAnalyzeResponse;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.script.dto.request.AiSalesScriptRetrieveRequest;
import cn.cordys.crm.ai.script.dto.response.ScriptRecommendResponse;
import cn.cordys.crm.ai.script.service.AiSalesScriptService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * AI 销售会话军师（功能 1）：粘贴文本 / 截图（多模态模型直接读图）→ 结构化分析（意向评分、成交信号、异议点、
 * 情绪、竞品提及、流失风险、候选话术）。复用 AgentChatService 的额度→模型→provider→计费四步链路，
 * 但自行构造 system+user、累积 chunk、按 {@code ai_advisor} 记账。
 */
@Service
@Slf4j
public class SalesAdvisorService {

    /** sys_parameter 里 PaddleOCR hubserving 服务地址 key */
    private static final String PARAM_OCR_SERVICE_URL = "ocr.serviceUrl";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 要求模型严格返回 JSON，字段见功能 1 PRD */
    private static final String SYSTEM_PROMPT = """
            你是资深 B2B 销售教练。请分析下面销售与客户的对话记录，先输出一段「分析结论」叙述（2-4 行，讲清客户意向强度、主要风险、下一步建议），空一行，再用 ```json 代码块输出严格 JSON（不要任何多余文字），字段如下：
            {
              "intentScore": "意向评分，0-100 的整数，仅数字",
              "signals": ["成交信号，数组；没有则为空数组"],
              "objections": ["客户异议点，数组；没有则为空数组"],
              "emotion": "客户情绪与满意度，一句话",
              "competitorMentions": ["竞品提及，数组；没有则为空数组"],
              "churnRisk": "流失风险，低/中/高 加一句话原因",
              "suggestedScripts": ["候选跟进话术，2-3 条，数组"]
            }
            只输出上述「分析结论」叙述 + JSON 代码块，不要其他内容。
            """;

    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private AttachmentService attachmentService;
    @Resource
    private BaseMapper<Parameter> parameterMapper;
    @Resource
    private AiSalesScriptService aiSalesScriptService;
    @Resource
    private AiAnalysisResultService aiAnalysisResultService;
    @Resource
    private CustomerService customerService;

    /**
     * 分析销售会话。截图（若有）直接作为图片喂给多模态视觉模型，再走四步链路调用模型并解析结构化结果。
     */
    public SalesAdvisorAnalyzeResponse analyze(String organizationId, SalesAdvisorAnalyzeRequest request) {
        return doAnalyze(organizationId, request, chunk -> { }).result();
    }

    /** 流式分析：先流式输出「分析结论」叙述，再解析 JSON 结构化字段，返回结果与用量。 */
    public AiStreamResult<SalesAdvisorAnalyzeResponse> analyzeStream(String organizationId,
            SalesAdvisorAnalyzeRequest request, Consumer<String> onChunk) {
        return doAnalyze(organizationId, request, onChunk);
    }

    private AiStreamResult<SalesAdvisorAnalyzeResponse> doAnalyze(String organizationId,
            SalesAdvisorAnalyzeRequest request, Consumer<String> onChunk) {
        if (StringUtils.isBlank(request.getMessage())
                && (request.getPicIds() == null || request.getPicIds().isEmpty())) {
            throw new GenericException("请粘贴聊天记录或上传截图");
        }

        // 1. 拼接输入：截图直接作为图片喂给多模态视觉模型（豆包），粘贴文本作为补充说明
        String userContent = request.getMessage() == null ? "" : request.getMessage().trim();
        List<String> imageUrls = new ArrayList<>();
        if (request.getPicIds() != null) {
            for (String picId : request.getPicIds()) {
                imageUrls.add(readImageDataUrl(picId));
            }
        }

        // 2. 模型候选 + 额度校验（会话军师属「洞察与评估任务」，走 taskModels 专用模型）
        List<AgentModel> models = agentModelService.resolveTaskModels(organizationId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }
        String userId = SessionUtils.getUserId();
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(organizationId);
        String status = quota.getStatus();
        if (isBlocked(status)) {
            throw new GenericException(blockedMessage(status));
        }

        // 3. 遍历候选调用，流式转发叙述部分并累积全文（首个模型一旦产出内容便不再降级）
        FencedJsonStreamer streamer = new FencedJsonStreamer(onChunk);
        AtomicBoolean emitted = new AtomicBoolean(false);
        Exception lastError = null;
        for (AgentModel model : models) {
            try {
                aiQuotaService.checkModelDailyLimit(organizationId, model, userId);
                LlmProvider provider = llmProviderFactory.get(model.getProvider());
                LlmChatRequest llmRequest = new LlmChatRequest();
                llmRequest.setModel(model.getModelName());
                llmRequest.setBaseUrl(model.getApiUrl());
                llmRequest.setApiKey(model.getApiKey());
                agentModelService.applyModelParams(llmRequest, model);
                llmRequest.setMessages(buildMessages(userContent, imageUrls));

                LlmUsage usage = provider.chatStream(llmRequest, chunk -> {
                    emitted.set(true);
                    streamer.accept(chunk);
                });
                aiQuotaService.record(organizationId, AiQuotaConstant.AI_ADVISOR, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                SalesAdvisorAnalyzeResponse analysis = parseAnalysis(streamer.text());
                enrichWithScriptLibrary(analysis, organizationId, request);
                persistAnalysisResult(organizationId, request.getCustomerId(), analysis, model.getModelName(), userId);
                return new AiStreamResult<>(analysis, usage);
            } catch (Exception e) {
                lastError = e;
                if (emitted.get()) {
                    log.error("军师模型调用中途失败，provider={}, model={}", model.getProvider(), model.getModelName(), e);
                    throw new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("军师模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }

        log.error("军师全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    /** 关联客户时把结构化结论沉淀到 ai_analysis_result，供客户画像回读；失败不阻断军师主流程 */
    private void persistAnalysisResult(String organizationId, String customerId,
            SalesAdvisorAnalyzeResponse analysis, String modelCode, String userId) {
        if (StringUtils.isBlank(customerId)) {
            return;
        }
        try {
            if (!customerService.existsInOrg(customerId, organizationId)) {
                log.warn("会话军师关联的客户不存在或不属于当前租户，跳过画像沉淀, customerId={}", customerId);
                return;
            }
            aiAnalysisResultService.save(organizationId, "customer", customerId,
                    AiQuotaConstant.AI_ADVISOR, "会话军师分析", JSON.toJSONString(analysis), modelCode, userId);
        } catch (Exception e) {
            log.warn("会话军师分析结果沉淀画像失败, customerId={}", customerId, e);
        }
    }

    /** 构造 system + user 消息；带截图时 user 消息携带图片（多模态） */
    private List<LlmMessage> buildMessages(String userContent, List<String> imageUrls) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", SYSTEM_PROMPT));
        if (imageUrls.isEmpty()) {
            messages.add(new LlmMessage("user", userContent));
        } else {
            String text = StringUtils.isBlank(userContent)
                    ? "请识别以下聊天记录截图，分析其中销售与客户的对话，并按系统要求输出结构化 JSON。"
                    : userContent;
            messages.add(new LlmMessage("user", text, imageUrls));
        }
        return messages;
    }

    // ==================== 结果解析 ====================

    private SalesAdvisorAnalyzeResponse parseAnalysis(String text) {
        SalesAdvisorAnalyzeResponse response = new SalesAdvisorAnalyzeResponse();
        String json = extractJson(text);
        if (json == null) {
            response.setRawAnalysis(text);
            return response;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            response.setIntentScore(textOf(root, "intentScore"));
            response.setSignals(strList(root, "signals"));
            response.setObjections(strList(root, "objections"));
            response.setEmotion(textOf(root, "emotion"));
            response.setCompetitorMentions(strList(root, "competitorMentions"));
            response.setChurnRisk(textOf(root, "churnRisk"));
            response.setSuggestedScripts(strList(root, "suggestedScripts"));
        } catch (Exception e) {
            log.warn("军师结果 JSON 解析失败，回退原文", e);
            response.setRawAnalysis(text);
        }
        return response;
    }

    /** 剥 ```json ... ``` 包裹，取首个 { 到末个 } 的 JSON 片段；无合法片段返回 null */
    private String extractJson(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int start = t.indexOf('\n');
            if (start < 0) {
                return null;
            }
            t = t.substring(start + 1);
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
            t = t.trim();
        }
        int begin = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (begin < 0 || end < 0 || end <= begin) {
            return null;
        }
        return t.substring(begin, end + 1);
    }

    private String textOf(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            return node.isEmpty() ? null : node.get(0).asText();
        }
        return node.asText();
    }

    private List<String> strList(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            List<String> list = new ArrayList<>();
            for (JsonNode item : node) {
                if (item != null && !item.isNull()) {
                    String s = item.isTextual() ? item.asText() : item.toString();
                    if (StringUtils.isNotBlank(s)) {
                        list.add(s.trim());
                    }
                }
            }
            return list;
        }
        String s = node.asText();
        return StringUtils.isBlank(s) ? List.of() : List.of(s.trim());
    }

    // ==================== 话术库增强 ====================

    /** 候选话术优先取话术库：检索成功则用带出处/原文的改写结果，否则保留模型自拟的 suggestedScripts */
    private void enrichWithScriptLibrary(SalesAdvisorAnalyzeResponse analysis, String organizationId,
            SalesAdvisorAnalyzeRequest request) {
        List<ScriptRecommendResponse> recommendations = retrieveLibraryScripts(organizationId, request,
                analysis.getObjections());
        if (recommendations.isEmpty()) {
            return;
        }
        analysis.setScriptRecommendations(recommendations);
        List<String> contents = new ArrayList<>();
        for (ScriptRecommendResponse recommendation : recommendations) {
            contents.add(recommendation.getContent());
        }
        analysis.setSuggestedScripts(contents);
    }

    /** 兜底检索话术库：话术库为空 / 检索失败时返回空列表，不阻断会话军师主流程 */
    private List<ScriptRecommendResponse> retrieveLibraryScripts(String organizationId, SalesAdvisorAnalyzeRequest request,
            List<String> objections) {
        String scenario = buildScriptScenario(request.getMessage(), objections);
        if (StringUtils.isBlank(scenario)) {
            return List.of();
        }
        AiSalesScriptRetrieveRequest retrieveRequest = new AiSalesScriptRetrieveRequest();
        retrieveRequest.setScenario(scenario);
        retrieveRequest.setTopK(3);
        try {
            return aiSalesScriptService.retrieve(retrieveRequest, organizationId);
        } catch (Exception e) {
            log.warn("会话军师检索话术库失败，回退模型自拟话术: {}", e.getMessage());
            return List.of();
        }
    }

    /** 用粘贴文本 + 客户异议点拼出话术检索场景（异议点是选话术的关键信号） */
    private String buildScriptScenario(String message, List<String> objections) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(message)) {
            parts.add(message.trim());
        }
        if (objections != null) {
            for (String objection : objections) {
                if (StringUtils.isNotBlank(objection)) {
                    parts.add("客户异议：" + objection.trim());
                }
            }
        }
        return String.join("；", parts);
    }

    // ==================== 截图 OCR ====================

    /** 截图 OCR 通道（已停用）：主路径改为多模态模型直接读图；保留作企微图片通道/纯文本模型降级兜底参考 */
    private String recognizeScreenshots(List<String> picIds) {
        String serviceUrl = getParam(PARAM_OCR_SERVICE_URL);
        if (StringUtils.isBlank(serviceUrl)) {
            throw new GenericException("截图识别服务未部署，请联系管理员");
        }
        StringBuilder sb = new StringBuilder();
        for (String picId : picIds) {
            sb.append(ocrPredict(serviceUrl, readImageBase64(picId))).append('\n');
        }
        return sb.toString().trim();
    }

    private byte[] readImageBytes(String picId) {
        try {
            ResponseEntity<org.springframework.core.io.Resource> res = attachmentService.getResource(picId);
            if (res == null || res.getBody() == null) {
                throw new GenericException("截图文件不存在，请重新上传");
            }
            try (InputStream in = res.getBody().getInputStream()) {
                return in.readAllBytes();
            }
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取截图失败, picId={}", picId, e);
            throw new GenericException("读取截图失败，请重新上传");
        }
    }

    private String readImageBase64(String picId) {
        return Base64.getEncoder().encodeToString(readImageBytes(picId));
    }

    /** 读附件并转成 data URL（data:<mime>;base64,...），供多模态视觉模型直接读图 */
    private String readImageDataUrl(String picId) {
        byte[] bytes = readImageBytes(picId);
        return "data:" + detectImageMime(bytes) + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    /** 按文件头魔数探测图片 MIME，兜底 image/png */
    private String detectImageMime(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return "image/png";
        }
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        if (b0 == 0xFF && b1 == 0xD8) {
            return "image/jpeg";
        }
        if (b0 == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "image/gif";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return "image/png";
    }

    /** 调 PaddleOCR hubserving 的 /predict/ocr_system，返回拼接的识别文本 */
    private String ocrPredict(String serviceUrl, String base64) {
        String url = serviceUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String endpoint = url + "/predict/ocr_system";
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("images", List.of(base64));
            String body = OBJECT_MAPPER.writeValueAsString(payload);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                throw new GenericException("截图识别失败（服务返回 " + resp.statusCode() + "）");
            }
            return parseOcrResult(resp.body());
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用截图识别服务失败, endpoint={}", endpoint, e);
            throw new GenericException("截图识别失败，请稍后重试");
        }
    }

    /** PaddleOCR 返回 {"results": [[{"text": "...", ...}]]}，拼接所有行文本 */
    private String parseOcrResult(String body) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            JsonNode page = results.get(0);
            if (page != null && page.isArray()) {
                for (JsonNode line : page) {
                    JsonNode textNode = line.get("text");
                    if (textNode != null && !textNode.isNull()) {
                        String text = textNode.asText().trim();
                        if (!text.isEmpty()) {
                            sb.append(text).append('\n');
                        }
                    }
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("解析截图识别结果失败: {}", body, e);
            return "";
        }
    }

    // ==================== 通用 ====================

    private String getParam(String key) {
        Parameter parameter = parameterMapper.selectByPrimaryKey(key);
        return parameter == null ? null : parameter.getParamValue();
    }

    private boolean isBlocked(String status) {
        return AiQuotaConstant.STATUS_HARD_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status);
    }

    private String blockedMessage(String status) {
        if (AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)) {
            return "调用过于频繁，请稍后再试";
        }
        if (AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status)) {
            return "AI 服务暂不可用，请联系管理员";
        }
        return "AI 额度已用完，请加购或升级";
    }
}
