package cn.cordys.crm.ai.advisor.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.advisor.AdvisorDataCatalog;
import cn.cordys.crm.ai.advisor.dto.request.AdvisorQueryRequest;
import cn.cordys.crm.ai.advisor.dto.response.AdvisorQueryResponse;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.conversation.service.AgentConversationService;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.security.SessionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 军师查数问答：大白话 → 查询意图 → 语义层编译只读 SQL（强制 organization_id + 本人及下属负责人范围）
 * → 执行 → 用结果生成自然语言回答。
 * <p>
 * 与问数 BI 的区别：数据域是「本人+下级负责的客户/跟进明细」，回答是自然语言（非报表），
 * 且行级权限强制限定在本人及下属名下。
 * </p>
 */
@Service
@Slf4j
public class AdvisorDataService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_ROWS = 100;
    private static final int CONTENT_MAX_LEN = 300;

    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private BaseService baseService;
    @Resource
    private AgentConversationService agentConversationService;
    @Resource
    private DataSource dataSource;

    public AdvisorQueryResponse query(String orgId, AdvisorQueryRequest request) {
        String question = request.getQuestion();
        if (StringUtils.isBlank(question)) {
            throw new GenericException("请输入你想查的客户数据问题");
        }
        String userId = SessionUtils.getUserId();

        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        if (isBlocked(quota.getStatus())) {
            throw new GenericException(blockedMessage(quota.getStatus()));
        }

        List<AgentModel> models = agentModelService.resolveChatModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }

        // 落会话，保证查数结果在左侧历史里可见、刷新后可回读（与分析一致）
        String runId = IDGenerator.nextStr();
        String assistantMessageId = IDGenerator.nextStr();
        String conversationId = agentConversationService.resolveConversation(orgId, userId,
                AiQuotaConstant.AI_ADVISOR, request.getConversationId(), title(question, "军师查数"));
        agentConversationService.saveUserMessage(conversationId, runId, orgId, userId, question.trim(), null);

        AdvisorQueryResponse response = new AdvisorQueryResponse();
        response.setQueried(true);
        response.setConversationId(conversationId);

        // 1. 意图解析（一次 LLM）
        IntentParseResult intentParse = parseIntent(orgId, userId, models, question);
        AdvisorDataCatalog.QueryIntent intent = intentParse.intent();
        LlmUsage usage = intentParse.usage() == null ? new LlmUsage() : intentParse.usage();

        if (AdvisorDataCatalog.ACTION_NONE.equals(intent.action())) {
            response.setQueried(false);
            response.setAnswer("这个我暂时只能查客户和跟进数据。你可以这样问："
                    + "「张三最近跟进到哪了」「我名下有哪些客户 7 天没跟进了」「公海里有哪些客户」");
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, orgId, userId,
                    response.getAnswer(), null, "done", usage);
            return response;
        }

        // 2. 编译执行只读 SQL（本人+下级负责人范围）
        List<String> ownerIds = new ArrayList<>(dataScopeService.getUserIdsWithSubordinates(userId, orgId));
        List<Map<String, Object>> readable = compileAndExecute(orgId, intent, ownerIds);

        // 3. 用结果生成自然语言回答（二次 LLM）
        AnswerResult answerResult = generateAnswer(orgId, userId, models, question, readable);
        usage = mergeUsage(usage, answerResult.usage());

        List<String> cols = columns(intent.action());
        response.setAnswer(answerResult.answer());
        response.setColumns(cols);
        response.setRows(toRows(readable, cols));

        agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, orgId, userId,
                answerResult.answer(), payloadJson(cols, readable), "done", usage);
        return response;
    }

    private record IntentParseResult(AdvisorDataCatalog.QueryIntent intent, LlmUsage usage) {
    }

    private record AnswerResult(String answer, LlmUsage usage) {
    }

    private record ModelCallResult(String text, LlmUsage usage) {
    }

    // ==================== 意图解析 ====================

    private IntentParseResult parseIntent(String orgId, String userId,
                                          List<AgentModel> models, String question) {
        List<LlmMessage> messages = List.of(
                new LlmMessage("system", AdvisorDataCatalog.intentPrompt()),
                new LlmMessage("user", question.trim()));

        Exception lastError = null;
        for (AgentModel model : models) {
            try {
                aiQuotaService.checkModelDailyLimit(orgId, model, userId);
                ModelCallResult call = callModel(orgId, userId, model, messages);
                return new IntentParseResult(parseIntentJson(call.text()), call.usage());
            } catch (GenericException e) {
                throw e;
            } catch (Exception e) {
                lastError = e;
                log.warn("军师查数意图解析模型调用失败，降级尝试下一个，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("军师查数意图解析全部模型失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    private AdvisorDataCatalog.QueryIntent parseIntentJson(String text) {
        String json = extractJson(text);
        if (json == null) {
            throw new GenericException("抱歉，没听懂这个问题，请换个问法试试");
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            String action = textOf(node.get("action"));
            if (StringUtils.isBlank(action)) {
                throw new GenericException("抱歉，没听懂这个问题，请换个问法试试");
            }
            String name = textOf(node.get("name"));
            Boolean sharedPool = node.hasNonNull("sharedPool") && !node.get("sharedPool").isNull()
                    ? node.get("sharedPool").asBoolean() : null;
            Integer staleDays = node.hasNonNull("staleDays") ? node.get("staleDays").asInt() : null;
            Integer withinDays = node.hasNonNull("withinDays") ? node.get("withinDays").asInt() : null;
            Integer limit = node.hasNonNull("limit") ? node.get("limit").asInt() : null;
            return new AdvisorDataCatalog.QueryIntent(action, name, sharedPool, staleDays, withinDays, limit);
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            throw new GenericException("抱歉，没听懂这个问题，请换个问法试试");
        }
    }

    // ==================== 编译执行 ====================

    private List<Map<String, Object>> compileAndExecute(String orgId, AdvisorDataCatalog.QueryIntent intent,
                                                        List<String> ownerIds) {
        String action = intent.action();
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String ownerIn = ownerIds.stream().map(u -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        long now = System.currentTimeMillis();

        switch (action) {
            case AdvisorDataCatalog.ACTION_LIST_CUSTOMERS -> {
                sql.append("SELECT c.name, c.owner, c.follow_time, c.follower, c.in_shared_pool ")
                        .append("FROM customer c WHERE c.organization_id = ? AND c.owner IN (").append(ownerIn).append(")");
                params.add(orgId);
                params.addAll(ownerIds);
                if (StringUtils.isNotBlank(intent.name())) {
                    sql.append(" AND c.name LIKE ?");
                    params.add("%" + intent.name().trim() + "%");
                }
                if (intent.sharedPool() != null) {
                    sql.append(" AND c.in_shared_pool = ?");
                    params.add(intent.sharedPool());
                }
                if (intent.staleDays() != null && intent.staleDays() > 0) {
                    long threshold = now - intent.staleDays() * 86_400_000L;
                    sql.append(" AND (c.follow_time IS NULL OR c.follow_time < ?)");
                    params.add(threshold);
                    sql.append(" ORDER BY c.follow_time IS NULL DESC, c.follow_time ASC");
                } else {
                    sql.append(" ORDER BY c.follow_time DESC");
                }
                sql.append(" LIMIT ?");
                params.add(AdvisorDataCatalog.normalizeLimit(intent.limit()));
            }
            case AdvisorDataCatalog.ACTION_CUSTOMER_FOLLOWS -> {
                if (StringUtils.isBlank(intent.name())) {
                    throw new GenericException("请说明要查哪个客户的跟进记录，例如「张三最近跟进到哪了」");
                }
                sql.append("SELECT c.name, f.follow_time, f.follow_method, f.content, f.owner ")
                        .append("FROM follow_up_record f JOIN customer c ON c.id = f.customer_id ")
                        .append("WHERE c.organization_id = ? AND c.owner IN (").append(ownerIn).append(")");
                params.add(orgId);
                params.addAll(ownerIds);
                sql.append(" AND c.name LIKE ?");
                params.add("%" + intent.name().trim() + "%");
                appendWithinDays(sql, params, intent.withinDays(), now, "f.follow_time");
                sql.append(" ORDER BY f.follow_time DESC LIMIT ?");
                params.add(AdvisorDataCatalog.normalizeLimit(intent.limit()));
            }
            case AdvisorDataCatalog.ACTION_RECENT_FOLLOWS -> {
                sql.append("SELECT c.name, f.follow_time, f.follow_method, f.content, f.owner ")
                        .append("FROM follow_up_record f JOIN customer c ON c.id = f.customer_id ")
                        .append("WHERE c.organization_id = ? AND c.owner IN (").append(ownerIn).append(")");
                params.add(orgId);
                params.addAll(ownerIds);
                appendWithinDays(sql, params, intent.withinDays(), now, "f.follow_time");
                sql.append(" ORDER BY f.follow_time DESC LIMIT ?");
                params.add(AdvisorDataCatalog.normalizeLimit(intent.limit()));
            }
            default -> throw new GenericException("抱歉，这个问题超出了我能查的范围");
        }

        List<Object[]> rows = executeQuery(sql.toString(), params);
        return humanize(action, rows, orgId);
    }

    private void appendWithinDays(StringBuilder sql, List<Object> params, Integer withinDays,
                                  long now, String column) {
        if (withinDays != null && withinDays > 0) {
            long from = now - withinDays * 86_400_000L;
            sql.append(" AND ").append(column).append(" >= ?");
            params.add(from);
        }
    }

    private List<Object[]> executeQuery(String sql, List<Object> params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setReadOnly(true);
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long l) {
                    ps.setLong(i + 1, l);
                } else if (p instanceof Integer in) {
                    ps.setInt(i + 1, in);
                } else if (p instanceof Boolean b) {
                    ps.setBoolean(i + 1, b);
                } else {
                    ps.setString(i + 1, p.toString());
                }
            }
            ps.setQueryTimeout(10);
            try (ResultSet rs = ps.executeQuery()) {
                List<Object[]> rows = new ArrayList<>();
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next() && rows.size() < MAX_ROWS) {
                    Object[] row = new Object[cols];
                    for (int c = 0; c < cols; c++) {
                        row[c] = rs.getObject(c + 1);
                    }
                    rows.add(row);
                }
                return rows;
            }
        } catch (SQLException e) {
            log.error("军师查数执行 SQL 失败, sql={}, params={}", sql, params, e);
            throw new GenericException("查询执行失败，请稍后重试或换个问法");
        }
    }

    /** 把原始行转成可读的 Map（姓名/时间/公海可读化），供 LLM 与前端复用。 */
    private List<Map<String, Object>> humanize(String action, List<Object[]> rows, String orgId) {
        List<String> cols = columns(action);
        // 收集需要转姓名的用户 id
        Set<String> userIds = new java.util.HashSet<>();
        for (Object[] row : rows) {
            for (int i = 0; i < cols.size(); i++) {
                String col = cols.get(i);
                if (isOwnerColumn(col) && row[i] != null) {
                    userIds.add(row[i].toString());
                }
            }
        }
        Map<String, String> userNameMap = userIds.isEmpty()
                ? Map.of() : baseService.getUserNameMap(new ArrayList<>(userIds));

        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < cols.size(); i++) {
                map.put(cols.get(i), humanizeValue(cols.get(i), row[i], userNameMap));
            }
            result.add(map);
        }
        return result;
    }

    private Object humanizeValue(String col, Object v, Map<String, String> userNameMap) {
        if (v == null) {
            return "";
        }
        if (isOwnerColumn(col)) {
            String name = userNameMap.get(v.toString());
            return StringUtils.isBlank(name) ? v.toString() : name;
        }
        if (isTimeColumn(col) && v instanceof Number n) {
            return Instant.ofEpochMilli(n.longValue()).atZone(ZoneId.systemDefault()).format(TIME_FMT);
        }
        if ("是否在公海".equals(col) && v instanceof Boolean b) {
            return b ? "是" : "否";
        }
        if ("跟进内容".equals(col) && v instanceof String s && s.length() > CONTENT_MAX_LEN) {
            return s.substring(0, CONTENT_MAX_LEN) + "…";
        }
        return v;
    }

    private boolean isOwnerColumn(String col) {
        return "负责人".equals(col) || "最近跟进人".equals(col) || "跟进人".equals(col);
    }

    private boolean isTimeColumn(String col) {
        return "最近跟进时间".equals(col) || "跟进时间".equals(col);
    }

    private List<String> columns(String action) {
        return AdvisorDataCatalog.ACTION_LIST_CUSTOMERS.equals(action)
                ? List.of("客户名称", "负责人", "最近跟进时间", "最近跟进人", "是否在公海")
                : List.of("客户名称", "跟进时间", "跟进方式", "跟进内容", "跟进人");
    }

    private List<List<Object>> toRows(List<Map<String, Object>> readable, List<String> cols) {
        List<List<Object>> rows = new ArrayList<>(readable.size());
        for (Map<String, Object> map : readable) {
            List<Object> row = new ArrayList<>(cols.size());
            for (String col : cols) {
                row.add(map.get(col));
            }
            rows.add(row);
        }
        return rows;
    }

    // ==================== 生成回答 ====================

    private AnswerResult generateAnswer(String orgId, String userId, List<AgentModel> models,
                                        String question, List<Map<String, Object>> readable) {
        String dataJson;
        try {
            dataJson = OBJECT_MAPPER.writeValueAsString(readable);
        } catch (Exception e) {
            dataJson = "[]";
        }
        List<LlmMessage> messages = List.of(
                new LlmMessage("system", AdvisorDataCatalog.answerPrompt()),
                new LlmMessage("user", "销售的问题：" + question.trim() + "\n\n查到的数据：\n" + dataJson));

        Exception lastError = null;
        for (AgentModel model : models) {
            try {
                aiQuotaService.checkModelDailyLimit(orgId, model, userId);
                ModelCallResult call = callModel(orgId, userId, model, messages);
                return new AnswerResult(call.text(), call.usage());
            } catch (GenericException e) {
                throw e;
            } catch (Exception e) {
                lastError = e;
                log.warn("军师查数回答生成模型调用失败，降级尝试下一个，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("军师查数回答生成全部模型失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    /** 同步调用一个模型拿到完整文本与用量，并记账额度。 */
    private ModelCallResult callModel(String orgId, String userId, AgentModel model, List<LlmMessage> messages)
            throws Exception {
        LlmProvider provider = llmProviderFactory.get(model.getProvider());
        LlmChatRequest llmRequest = new LlmChatRequest();
        llmRequest.setModel(model.getModelName());
        llmRequest.setBaseUrl(model.getApiUrl());
        llmRequest.setApiKey(model.getApiKey());
        agentModelService.applyModelParams(llmRequest, model);
        llmRequest.setMessages(messages);

        StringBuilder sb = new StringBuilder();
        LlmUsage usage = provider.chatStream(llmRequest, sb::append);
        aiQuotaService.record(orgId, AiQuotaConstant.AI_ADVISOR, model.getModelName(),
                usage.getInputTokens(), usage.getOutputTokens(), userId);
        return new ModelCallResult(sb.toString(), usage);
    }

    // ==================== 通用 ====================

    /** 会话标题：取问题前 20 字，空则回退默认标题。 */
    private String title(String text, String fallback) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return fallback;
        }
        return t.length() > 20 ? t.substring(0, 20) : t;
    }

    private LlmUsage mergeUsage(LlmUsage base, LlmUsage extra) {
        if (base == null) {
            base = new LlmUsage();
        }
        if (extra != null) {
            base.setInputTokens(base.getInputTokens() + extra.getInputTokens());
            base.setOutputTokens(base.getOutputTokens() + extra.getOutputTokens());
            base.setTotalTokens(base.getTotalTokens() + extra.getTotalTokens());
        }
        return base;
    }

    /** 查数结果的落库 payload：列名 + 可读化二维行，供前端历史回读渲染数据来源。 */
    private String payloadJson(List<String> cols, List<Map<String, Object>> readable) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("columns", cols);
            payload.put("rows", toRows(readable, cols));
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private String textOf(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private String extractJson(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        String t = text.trim();
        int s = t.indexOf('{');
        int e = t.lastIndexOf('}');
        return (s < 0 || e <= s) ? null : t.substring(s, e + 1);
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
