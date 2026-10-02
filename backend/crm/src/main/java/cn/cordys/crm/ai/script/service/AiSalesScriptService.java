package cn.cordys.crm.ai.script.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.script.domain.AiSalesScript;
import cn.cordys.crm.ai.script.dto.request.AiSalesScriptPageRequest;
import cn.cordys.crm.ai.script.dto.request.AiSalesScriptRetrieveRequest;
import cn.cordys.crm.ai.script.dto.request.AiSalesScriptSaveRequest;
import cn.cordys.crm.ai.script.dto.response.AiSalesScriptResponse;
import cn.cordys.crm.ai.script.dto.response.ScriptRecommendResponse;
import cn.cordys.crm.ai.script.mapper.ExtAiSalesScriptMapper;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 销售话术库服务（功能 2）：话术 CRUD + 检索改写。检索复用会话军师的额度→模型→provider→计费四步链路，
 * 先按租户/分类粗筛候选，再让模型挑选最贴合的话术并改写；出处由服务端按候选编号回填，避免模型编造。
 */
@Service
@Slf4j
public class AiSalesScriptService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 检索候选上限（话术库规模小，按更新时间取最新若干条，量大时前端先选分类收窄） */
    private static final int CANDIDATE_LIMIT = 50;

    /** 候选话术内容在 prompt 中的截断长度，控制 token 成本 */
    private static final int CONTENT_TRUNCATE = 200;

    private static final int DEFAULT_TOP_K = 3;

    private static final String DEFAULT_SOURCE = "租户自建";

    private static final String SYSTEM_PROMPT = """
            你是资深 B2B 销售教练。请根据客户场景，从候选话术列表中挑选最贴合的话术，并把每条话术改写成可直接发给客户的话术（保留原意、贴合场景、语气自然）。输出严格 JSON（不要 markdown 代码块、不要任何多余文字），结构如下：
            {
              "recommendations": [
                {"index": 候选编号, "script": "改写后的话术"}
              ]
            }
            只输出上述 JSON 本身，不要任何前后缀。
            """;

    @Resource
    private BaseMapper<AiSalesScript> scriptMapper;
    @Resource
    private ExtAiSalesScriptMapper extAiSalesScriptMapper;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;

    // ==================== 话术维护 ====================

    public Pager<List<AiSalesScriptResponse>> page(AiSalesScriptPageRequest request, String orgId) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<AiSalesScriptResponse> list = extAiSalesScriptMapper.selectPage(
                orgId, request.getKeyword(), request.getCategory());
        return PageUtils.setPageInfo(page, list);
    }

    public List<String> categories(String orgId) {
        return extAiSalesScriptMapper.selectCategories(orgId);
    }

    public void add(AiSalesScriptSaveRequest request, String orgId, String userId) {
        validateSave(request);
        long now = System.currentTimeMillis();
        AiSalesScript script = new AiSalesScript();
        script.setId(IDGenerator.nextStr());
        script.setOrganizationId(orgId);
        script.setCategory(trimToNull(request.getCategory()));
        script.setTitle(request.getTitle().trim());
        script.setContent(request.getContent().trim());
        script.setSource(resolveSource(request.getSource()));
        script.setCreateUser(userId);
        script.setUpdateUser(userId);
        script.setCreateTime(now);
        script.setUpdateTime(now);
        scriptMapper.insert(script);
    }

    public void update(AiSalesScriptSaveRequest request, String orgId, String userId) {
        AiSalesScript script = checkScript(request.getId(), orgId);
        validateSave(request);
        script.setCategory(trimToNull(request.getCategory()));
        script.setTitle(request.getTitle().trim());
        script.setContent(request.getContent().trim());
        script.setSource(resolveSource(request.getSource()));
        script.setUpdateUser(userId);
        script.setUpdateTime(System.currentTimeMillis());
        scriptMapper.updateById(script);
    }

    public void delete(String id, String orgId) {
        checkScript(id, orgId);
        scriptMapper.deleteByPrimaryKey(id);
    }

    // ==================== 检索改写 ====================

    public List<ScriptRecommendResponse> retrieve(AiSalesScriptRetrieveRequest request, String orgId) {
        if (StringUtils.isBlank(request.getScenario())) {
            throw new GenericException("请输入客户场景");
        }
        int topK = request.getTopK() == null || request.getTopK() <= 0 ? DEFAULT_TOP_K : request.getTopK();

        List<AiSalesScript> candidates = extAiSalesScriptMapper.selectCandidates(
                orgId, request.getCategory(), CANDIDATE_LIMIT);
        if (candidates.isEmpty()) {
            throw new GenericException("话术库为空，请先维护话术");
        }

        List<AgentModel> models = agentModelService.resolveChatModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }
        String userId = SessionUtils.getUserId();
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        String status = quota.getStatus();
        if (isBlocked(status)) {
            throw new GenericException(blockedMessage(status));
        }

        StringBuilder content = new StringBuilder();
        AtomicBoolean emitted = new AtomicBoolean(false);
        Exception lastError = null;
        for (AgentModel model : models) {
            try {
                aiQuotaService.checkModelDailyLimit(orgId, model, userId);
                LlmProvider provider = llmProviderFactory.get(model.getProvider());
                LlmChatRequest llmRequest = new LlmChatRequest();
                llmRequest.setModel(model.getModelName());
                llmRequest.setBaseUrl(model.getApiUrl());
                llmRequest.setApiKey(model.getApiKey());
                llmRequest.setMessages(buildMessages(request.getScenario().trim(), candidates, topK));

                LlmUsage usage = provider.chatStream(llmRequest, chunk -> {
                    emitted.set(true);
                    content.append(chunk);
                });
                aiQuotaService.record(orgId, AiQuotaConstant.AI_SALES_RAG, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return parseRecommendations(content.toString(), candidates, topK);
            } catch (Exception e) {
                lastError = e;
                if (emitted.get()) {
                    log.error("话术检索模型调用中途失败，provider={}, model={}", model.getProvider(), model.getModelName(), e);
                    throw new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("话术检索模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }

        log.error("话术检索全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    // ==================== 内部方法 ====================

    private void validateSave(AiSalesScriptSaveRequest request) {
        if (StringUtils.isBlank(request.getTitle())) {
            throw new GenericException("请填写话术标题");
        }
        if (StringUtils.isBlank(request.getContent())) {
            throw new GenericException("请填写话术内容");
        }
    }

    private AiSalesScript checkScript(String id, String orgId) {
        AiSalesScript script = scriptMapper.selectByPrimaryKey(id);
        if (script == null || !orgId.equals(script.getOrganizationId())) {
            throw new GenericException("话术不存在");
        }
        return script;
    }

    private String resolveSource(String source) {
        return StringUtils.isBlank(source) ? DEFAULT_SOURCE : source.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private List<LlmMessage> buildMessages(String scenario, List<AiSalesScript> candidates, int topK) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", SYSTEM_PROMPT));

        StringBuilder sb = new StringBuilder();
        sb.append("客户场景：").append(scenario).append("\n\n候选话术：\n");
        for (int i = 0; i < candidates.size(); i++) {
            AiSalesScript c = candidates.get(i);
            sb.append(i + 1).append(". ").append(c.getTitle()).append('\n')
                    .append(truncate(c.getContent())).append('\n');
        }
        sb.append("\n请从上述候选话术中挑选最贴合的 ").append(topK).append(" 条，按系统要求输出 JSON。");
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    private String truncate(String content) {
        if (content == null) {
            return "";
        }
        String t = content.trim();
        return t.length() <= CONTENT_TRUNCATE ? t : t.substring(0, CONTENT_TRUNCATE) + "…";
    }

    /** 解析模型返回 JSON，用候选编号反查出处/标题/原文，保证出处权威不被模型编造 */
    private List<ScriptRecommendResponse> parseRecommendations(String text, List<AiSalesScript> candidates, int topK) {
        List<ScriptRecommendResponse> result = new ArrayList<>();
        String json = extractJson(text);
        if (json == null) {
            throw new GenericException("AI 未能生成话术推荐，请重试");
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode recs = root.get("recommendations");
            if (recs == null || !recs.isArray()) {
                throw new GenericException("AI 未能生成话术推荐，请重试");
            }
            for (JsonNode rec : recs) {
                int index = rec.path("index").asInt(-1);
                String script = rec.path("script").asText(null);
                if (index <= 0 || index > candidates.size() || StringUtils.isBlank(script)) {
                    continue;
                }
                AiSalesScript original = candidates.get(index - 1);
                ScriptRecommendResponse item = new ScriptRecommendResponse();
                item.setTitle(original.getTitle());
                item.setContent(script.trim());
                item.setSource(original.getSource());
                item.setOriginalContent(original.getContent());
                result.add(item);
                if (result.size() >= topK) {
                    break;
                }
            }
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            log.warn("话术推荐结果 JSON 解析失败", e);
            throw new GenericException("AI 未能生成话术推荐，请重试");
        }
        if (result.isEmpty()) {
            throw new GenericException("AI 未能生成话术推荐，请重试");
        }
        return result;
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
