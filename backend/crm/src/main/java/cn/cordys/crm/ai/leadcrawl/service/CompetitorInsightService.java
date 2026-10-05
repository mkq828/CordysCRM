package cn.cordys.crm.ai.leadcrawl.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.leadcrawl.dto.request.CompetitorInsightRequest;
import cn.cordys.crm.ai.leadcrawl.dto.response.CompetitorInsightResponse;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.ai.service.FencedJsonStreamer;
import cn.cordys.security.SessionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 竞品/行业洞察（功能 10 第二块）：纯 LLM 分析，输入竞品名/行业/关注点，输出结构化简报。
 * 走任务模型（洞察与评估），按 {@code lead_crawl} 记账，挂 G2 额度 + G3 版本门控；结果不落库。
 */
@Service
@Slf4j
public class CompetitorInsightService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 要求模型输出「叙述 + fenced JSON」，字段见功能 10 PRD */
    private static final String SYSTEM_PROMPT = """
            你是资深 B2B 市场与竞争情报分析师。请基于你对公开商业信息的了解，围绕用户提供的竞品与行业，生成一份竞品/行业洞察简报。
            写作铁律：
            1. 只输出可落地的判断与建议，不堆砌形容词。
            2. 信息若有不确定性，用「据公开信息/据行业通识」等措辞，不编造精确数据。
            3. 风险与机会并重，建议要能直接指导销售或产品动作。

            先输出一段「洞察结论」叙述（2-4 行，概括竞品核心动态、主要威胁、关键机会），空一行，再用 ```json 代码块输出严格 JSON（不要任何多余文字），字段如下：
            {
              "overview": "竞品概况，一句话",
              "dynamics": ["近期动态，数组；没有则为空数组"],
              "trends": ["行业趋势，数组"],
              "risks": ["风险提示，数组"],
              "suggestions": ["跟进/应对建议，2-4 条，数组"]
            }
            只输出上述「洞察结论」叙述 + JSON 代码块，不要其他内容。
            """;

    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;

    public AiStreamResult<CompetitorInsightResponse> insight(String organizationId,
            CompetitorInsightRequest request, Consumer<String> onChunk) {
        if (StringUtils.isBlank(request.getCompetitor()) && StringUtils.isBlank(request.getIndustry())) {
            throw new GenericException("请填写竞品名称或所属行业");
        }

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
                llmRequest.setMessages(buildMessages(request));

                LlmUsage usage = provider.chatStream(llmRequest, chunk -> {
                    emitted.set(true);
                    streamer.accept(chunk);
                });
                aiQuotaService.record(organizationId, AiQuotaConstant.AI_LEAD_CRAWL, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return new AiStreamResult<>(parseResult(streamer.text()), usage);
            } catch (Exception e) {
                lastError = e;
                if (emitted.get()) {
                    log.error("竞品洞察模型调用中途失败，provider={}, model={}", model.getProvider(), model.getModelName(), e);
                    throw new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("竞品洞察模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }

        log.error("竞品洞察全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    private List<LlmMessage> buildMessages(CompetitorInsightRequest request) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", SYSTEM_PROMPT));
        StringBuilder sb = new StringBuilder();
        sb.append("竞品：").append(StringUtils.trimToEmpty(request.getCompetitor())).append('\n')
                .append("行业：").append(StringUtils.trimToEmpty(request.getIndustry())).append('\n')
                .append("关注点：").append(StringUtils.trimToEmpty(request.getFocus())).append('\n')
                .append("请生成竞品/行业洞察简报。");
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    private CompetitorInsightResponse parseResult(String text) {
        CompetitorInsightResponse response = new CompetitorInsightResponse();
        String json = extractJson(text);
        if (json == null) {
            response.setRawContent(text);
            return response;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            response.setOverview(root.path("overview").asText(null));
            response.setDynamics(strList(root, "dynamics"));
            response.setTrends(strList(root, "trends"));
            response.setRisks(strList(root, "risks"));
            response.setSuggestions(strList(root, "suggestions"));
        } catch (Exception e) {
            log.warn("竞品洞察结果 JSON 解析失败，回退原文", e);
            response.setRawContent(text);
        }
        return response;
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
