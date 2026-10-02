package cn.cordys.crm.ai.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.request.SalesAdvisorAnalyzeRequest;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.dto.response.SalesAdvisorAnalyzeResponse;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
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

/**
 * AI 销售会话军师（功能 1）：粘贴文本 / 截图 OCR → 结构化分析（意向评分、成交信号、异议点、
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
            你是资深 B2B 销售教练。请分析下面销售与客户的对话记录，输出严格 JSON（不要 markdown 代码块、不要任何多余文字），字段如下：
            {
              "intentScore": "意向评分，0-100 的整数，仅数字",
              "signals": ["成交信号，数组；没有则为空数组"],
              "objections": ["客户异议点，数组；没有则为空数组"],
              "emotion": "客户情绪与满意度，一句话",
              "competitorMentions": ["竞品提及，数组；没有则为空数组"],
              "churnRisk": "流失风险，低/中/高 加一句话原因",
              "suggestedScripts": ["候选跟进话术，2-3 条，数组"]
            }
            只输出上述 JSON 本身，不要任何前后缀。
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

    /**
     * 分析销售会话。截图（若有）先走 OCR 识别文本并入内容，再走四步链路调用模型并解析结构化结果。
     */
    public SalesAdvisorAnalyzeResponse analyze(String organizationId, SalesAdvisorAnalyzeRequest request) {
        if (StringUtils.isBlank(request.getMessage())
                && (request.getPicIds() == null || request.getPicIds().isEmpty())) {
            throw new GenericException("请粘贴聊天记录或上传截图");
        }

        // 1. 拼接输入：截图 OCR 文本 + 粘贴文本
        String userContent = request.getMessage() == null ? "" : request.getMessage().trim();
        if (request.getPicIds() != null && !request.getPicIds().isEmpty()) {
            String ocrText = recognizeScreenshots(request.getPicIds());
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotBlank(ocrText)) {
                sb.append("【截图识别文本】\n").append(ocrText);
            }
            if (StringUtils.isNotBlank(userContent)) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append("【粘贴的聊天记录】\n").append(userContent);
            }
            userContent = sb.toString();
        }

        // 2. 模型候选 + 额度校验
        List<AgentModel> models = agentModelService.resolveChatModels(organizationId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }
        String userId = SessionUtils.getUserId();
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(organizationId);
        String status = quota.getStatus();
        if (isBlocked(status)) {
            throw new GenericException(blockedMessage(status));
        }

        // 3. 遍历候选调用，累积 chunk（首个模型一旦产出内容便不再降级）
        StringBuilder content = new StringBuilder();
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
                llmRequest.setMessages(List.of(
                        new LlmMessage("system", SYSTEM_PROMPT),
                        new LlmMessage("user", userContent)));

                LlmUsage usage = provider.chatStream(llmRequest, chunk -> {
                    emitted.set(true);
                    content.append(chunk);
                });
                aiQuotaService.record(organizationId, AiQuotaConstant.AI_ADVISOR, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return parseAnalysis(content.toString());
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

    // ==================== 截图 OCR ====================

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

    private String readImageBase64(String picId) {
        try {
            ResponseEntity<org.springframework.core.io.Resource> res = attachmentService.getResource(picId);
            if (res == null || res.getBody() == null) {
                throw new GenericException("截图文件不存在，请重新上传");
            }
            try (InputStream in = res.getBody().getInputStream()) {
                return Base64.getEncoder().encodeToString(in.readAllBytes());
            }
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取截图失败, picId={}", picId, e);
            throw new GenericException("读取截图失败，请重新上传");
        }
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
