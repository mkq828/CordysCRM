package cn.cordys.crm.ai.bi.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.ai.bi.BiCatalog;
import cn.cordys.crm.ai.bi.dto.request.AskBiQueryRequest;
import cn.cordys.crm.ai.bi.dto.response.AskBiQueryResponse;
import cn.cordys.crm.ai.bi.dto.response.BiCatalogResponse;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 问数 BI（功能 12）：大白话 → 查询意图 JSON → 语义层编译 SQL → 只读执行 → 返回结果。
 * 只生成 SELECT、标识符全部来自 {@link BiCatalog} 白名单、值全部参数化、无条件注入 organization_id。
 */
@Service
@Slf4j
public class AskBiService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_ROWS = 500;

    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private DataSource dataSource;

    /** LLM 产出的查询意图（metric/dimension 必须命中目录 key，timeRange 为闭区间日期）。 */
    private record BiQueryIntent(String metric, String dimension, String order, Integer limit,
                                 String chartType, String title, TimeRange timeRange) {
    }

    private record TimeRange(String from, String to) {
    }

    public BiCatalogResponse catalog() {
        BiCatalogResponse response = new BiCatalogResponse();
        response.setMetrics(BiCatalog.METRICS.values().stream()
                .map(m -> new BiCatalogResponse.CatalogItem(m.key(), m.name())).toList());
        response.setSampleQuestions(BiCatalog.SAMPLE_QUESTIONS);
        response.setScopeHint(BiCatalog.SCOPE_HINT);
        return response;
    }

    public AskBiQueryResponse query(String orgId, AskBiQueryRequest request) {
        return doQuery(orgId, request, true);
    }

    /**
     * 数据大屏「AI 经营助手」只读问数：复用同一语义层（BiCatalog + SQL 编译），
     * 但免费全租户、不计量额度——只做模型调用与查询，不检查企业版 featureCode / 额度 / 模型日限额。
     * 权限在 Controller 层由 DASHBOARD_READ 门控。
     */
    public AskBiQueryResponse queryFree(String orgId, AskBiQueryRequest request) {
        return doQuery(orgId, request, false);
    }

    private AskBiQueryResponse doQuery(String orgId, AskBiQueryRequest request, boolean metered) {
        String question = request.getQuestion();
        if (StringUtils.isBlank(question)) {
            throw new GenericException("请输入您想问的数据问题");
        }
        String userId = SessionUtils.getUserId();
        if (metered) {
            AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
            if (isBlocked(quota.getStatus())) {
                throw new GenericException(blockedMessage(quota.getStatus()));
            }
        }

        List<AgentModel> models = agentModelService.resolveChatModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }

        Exception lastError = null;
        AtomicBoolean consumed = new AtomicBoolean(false);
        for (AgentModel model : models) {
            try {
                if (metered) {
                    aiQuotaService.checkModelDailyLimit(orgId, model, userId);
                }
                LlmProvider provider = llmProviderFactory.get(model.getProvider());
                LlmChatRequest llmRequest = new LlmChatRequest();
                llmRequest.setModel(model.getModelName());
                llmRequest.setBaseUrl(model.getApiUrl());
                llmRequest.setApiKey(model.getApiKey());
                agentModelService.applyModelParams(llmRequest, model);
                llmRequest.setMessages(buildMessages(question));

                StringBuilder sb = new StringBuilder();
                LlmUsage usage = provider.chatStream(llmRequest, sb::append);
                consumed.set(true);
                if (metered) {
                    aiQuotaService.record(orgId, AiQuotaConstant.AI_ASK_BI, model.getModelName(),
                            usage.getInputTokens(), usage.getOutputTokens(), userId);
                }

                BiQueryIntent intent = parseIntent(sb.toString());
                return compileAndExecute(orgId, intent);
            } catch (Exception e) {
                lastError = e;
                if (consumed.get()) {
                    // 模型已消费：解析/编译/执行失败不再降级换模型，直接给可读错误
                    throw e instanceof GenericException ? (GenericException) e
                            : new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("问数 BI 模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("问数 BI 全部模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    private List<LlmMessage> buildMessages(String question) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", systemPrompt()));
        messages.add(new LlmMessage("user", question.trim()));
        return messages;
    }

    private String systemPrompt() {
        return "你是企业 CRM 的「问数助手」，把老板/销售的大白话数据问题转成一段 JSON，交给系统查数据库。\n"
                + "今天日期：" + LocalDate.now() + "\n\n"
                + "只支持以下指标（key = 名称）：\n"
                + "- contract_amount = 合同金额\n"
                + "- contract_count = 合同数\n"
                + "- contract_avg_amount = 平均合同额\n"
                + "- payment_amount = 回款金额\n"
                + "- payment_verified_amount = 已核销回款金额\n"
                + "- customer_count = 客户数\n\n"
                + "只支持以下维度（key = 名称，必须是指标支持的维度，拿不准就省略 dimension 直接查总量）：\n"
                + "- customer = 按客户\n"
                + "- owner = 按负责人\n"
                + "- contract_stage = 按合同阶段\n"
                + "- contract_approval = 按审批状态\n"
                + "- payment_verify_status = 按回款核销状态\n"
                + "- time_day / time_week / time_month = 按日期 / 周 / 月\n\n"
                + "相对时间（上月/本月/近6个月/今年等）要换算成具体日期，from 与 to 均为 yyyy-MM-dd 闭区间（含当天）。\n"
                + "输出只能是 JSON（不要 markdown、不要解释），结构如下：\n"
                + "{\"metric\":\"payment_amount\",\"dimension\":\"customer\",\"timeRange\":{\"from\":\"2026-09-01\",\"to\":\"2026-09-30\"},\"order\":\"DESC\",\"limit\":10,\"chartType\":\"bar\",\"title\":\"上月各客户回款金额 Top10\"}\n"
                + "字段说明：metric 必填；dimension 可选（无则查总量）；timeRange 可选（无则查全部时间）；"
                + "order 为 DESC/ASC（Top 问题用 DESC）；limit 为返回条数（默认 10，最大 500）；"
                + "chartType 为 bar/line/pie/table（按客户/负责人用 bar，按时间用 line，占比用 pie，明细用 table）；title 为简短中文标题。";
    }

    private BiQueryIntent parseIntent(String text) {
        String json = extractJson(text);
        if (json == null) {
            throw new GenericException("抱歉，没听懂这个问题，请换个问法试试");
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            String metric = text(node.get("metric"));
            if (StringUtils.isBlank(metric)) {
                throw new GenericException("抱歉，没听懂这个问题，请换个问法试试");
            }
            String dimension = text(node.get("dimension"));
            String order = text(node.get("order"));
            Integer limit = node.hasNonNull("limit") ? node.get("limit").asInt() : null;
            String chartType = text(node.get("chartType"));
            String title = text(node.get("title"));
            JsonNode tr = node.get("timeRange");
            TimeRange timeRange = null;
            if (tr != null && tr.isObject()) {
                timeRange = new TimeRange(text(tr.get("from")), text(tr.get("to")));
            }
            return new BiQueryIntent(metric, dimension, order, limit, chartType, title, timeRange);
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            throw new GenericException("抱歉，没听懂这个问题，请换个问法试试");
        }
    }

    private AskBiQueryResponse compileAndExecute(String orgId, BiQueryIntent intent) {
        BiCatalog.Metric metric = BiCatalog.METRICS.get(intent.metric());
        if (metric == null) {
            throw new GenericException("抱歉，这个问题超出了我能查的范围。可试试："
                    + "上月各客户回款金额 Top10、本月新签合同额、近 6 个月回款趋势");
        }
        BiCatalog.Dimension dim = null;
        if (StringUtils.isNotBlank(intent.dimension())) {
            dim = metric.dimensions().get(intent.dimension());
            if (dim == null) {
                List<String> supported = metric.dimensions().values().stream()
                        .map(BiCatalog.Dimension::name).toList();
                throw new GenericException("「" + metric.name() + "」暂不支持按「" + intent.dimension()
                        + "」分组，支持的维度：" + String.join("、", supported));
            }
        }

        StringBuilder sql = new StringBuilder("SELECT ");
        List<Object> params = new ArrayList<>();
        if (dim != null) {
            sql.append(dim.selectSql()).append(" AS dim_value, ");
        }
        sql.append(metric.selectSql()).append(" AS metric_value FROM ").append(metric.fromSql());
        sql.append(" WHERE ").append(metric.orgColumn()).append(" = ?");
        params.add(orgId);
        if (StringUtils.isNotBlank(metric.whereSql())) {
            sql.append(" AND ").append(metric.whereSql());
        }
        TimeRange tr = intent.timeRange();
        if (tr != null && StringUtils.isNotBlank(tr.from()) && StringUtils.isNotBlank(tr.to())
                && metric.timeColumn() != null) {
            long fromMs = parseDayMillis(tr.from());
            long toMs = parseDayMillis(tr.to()) + 86_400_000L;
            sql.append(" AND ").append(metric.timeColumn()).append(" >= ? AND ").append(metric.timeColumn()).append(" < ?");
            params.add(fromMs);
            params.add(toMs);
        }
        if (dim != null) {
            sql.append(" GROUP BY ").append(dim.groupSql());
            if (intent.dimension().startsWith("time_")) {
                sql.append(" ORDER BY dim_value ASC");
            } else {
                sql.append(" ORDER BY metric_value ").append("DESC".equalsIgnoreCase(intent.order()) ? "DESC" : "ASC");
            }
            int limit = intent.limit() == null ? 10 : Math.min(Math.max(intent.limit(), 1), MAX_ROWS);
            sql.append(" LIMIT ?");
            params.add(limit);
        }

        List<Object[]> rows = executeQuery(sql.toString(), params);

        AskBiQueryResponse response = new AskBiQueryResponse();
        response.setTitle(StringUtils.isBlank(intent.title()) ? defaultTitle(metric, dim) : intent.title());
        response.setChartType(resolveChartType(intent, dim));
        response.setColumns(columns(metric, dim));
        response.setRows(normalizeRows(rows));
        return response;
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
            log.error("问数 BI 执行 SQL 失败, sql={}, params={}", sql, params, e);
            throw new GenericException("查询执行失败，请稍后重试或换个问法");
        }
    }

    private List<String> columns(BiCatalog.Metric metric, BiCatalog.Dimension dim) {
        List<String> columns = new ArrayList<>();
        if (dim != null) {
            columns.add(dim.name());
        }
        columns.add(metric.name());
        return columns;
    }

    private List<List<Object>> normalizeRows(List<Object[]> rows) {
        List<List<Object>> data = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            List<Object> list = new ArrayList<>(row.length);
            for (Object o : row) {
                list.add(o instanceof BigDecimal bd ? bd.setScale(2, RoundingMode.HALF_UP) : o);
            }
            data.add(list);
        }
        return data;
    }

    private String resolveChartType(BiQueryIntent intent, BiCatalog.Dimension dim) {
        if (dim == null) {
            return "number";
        }
        String ct = intent.chartType();
        return switch (ct == null ? "" : ct) {
            case "bar", "line", "pie", "table" -> ct;
            default -> "table";
        };
    }

    private String defaultTitle(BiCatalog.Metric metric, BiCatalog.Dimension dim) {
        return dim == null ? metric.name() : "按" + dim.name() + "的" + metric.name();
    }

    private long parseDayMillis(String date) {
        try {
            return LocalDate.parse(date.trim()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            throw new GenericException("时间范围无法识别，请换个问法试试");
        }
    }

    private String text(JsonNode node) {
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
