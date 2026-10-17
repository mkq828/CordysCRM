package cn.cordys.crm.ai.employee.action.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.mapper.ExtAiEmployeeActionMapper;
import cn.cordys.crm.ai.knowledge.dto.response.AiKnowledgeCitation;
import cn.cordys.crm.ai.knowledge.service.AiKnowledgeService;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 员工「自动跑任务」各场景扫描的共享骨架（功能 6 二期）。
 * <p>
 * 模板方法编排统一闭环：额度校验 → task 模型解析 → 查候选资源 → 按 actionType+resourceId 去重/静默 →
 * 逐个走任务模型生成草稿 → 落 PENDING 动作 → 按负责人站内信通知。各场景只实现钩子（候选 SQL / 提示词 /
 * 载荷解析 / 通知），写库前的权限复核统一在 {@link AiAgentActionExecutor} 各分支完成。
 * </p>
 * <p>
 * 无登录态（定时任务）时 userId 传 null，额度按租户口径计量；手动触发时以当前登录用户计量。
 * </p>
 */
@Slf4j
public abstract class AbstractAiAutoScanService {

    /** 毫秒/天，静默期与阈值换算用 */
    protected static final long DAY_MILLIS = 86_400_000L;

    /** 通用候选：各场景扫描结果统一抽象，业务差异经 {@link #extra()} 传递 */
    protected record Candidate(
            String resourceType,
            String resourceId,
            String resourceName,
            String customerId,
            String customerName,
            String ownerId,
            String title,
            Map<String, Object> extra) {
    }

    @Resource
    protected ExtAiEmployeeActionMapper extAiEmployeeActionMapper;

    @Resource
    protected BaseMapper<AiEmployeeAction> actionMapper;

    @Resource
    protected AgentModelService agentModelService;

    @Resource
    protected AiQuotaService aiQuotaService;

    @Resource
    protected LlmProviderFactory llmProviderFactory;

    @Resource
    protected AiKnowledgeService aiKnowledgeService;

    @Resource
    protected BaseMapper<Parameter> parameterMapper;

    /** 动作类型（如 FOLLOW_UP_CREATE / DUNNING / TAG），同时作为按类型手动触发的路由 key */
    public abstract String actionType();

    /** 该场景给模型的系统提示词 */
    protected abstract String systemPrompt();

    /** 查询本场景候选资源（已按 scanLimit 截断） */
    protected abstract List<Candidate> resolveCandidates(String orgId);

    /** 组装用户提示词；kbContext 为按 {@link #knowledgeQuestion} 检索到的知识库参考，场景自行决定是否拼入 */
    protected abstract String buildPrompt(String orgId, Candidate candidate, String kbContext);

    /** 解析并校验 LLM 输出的 JSON，返回落库 payload（非法时抛异常跳过该候选） */
    protected abstract Map<String, Object> parsePayload(String json);

    /** 通知负责人（按负责人聚合后逐个回调） */
    protected abstract void notifyOwner(String orgId, String ownerId, int count);

    /** 每次扫描最多生成的候选数，防一次扫太多烧额度 */
    protected int scanLimit() {
        return intParam("ai.auto_task.scan_limit", 10);
    }

    /** 驳回静默天数：驳回后该资源在静默期内不再重复生成同类型建议 */
    protected int quietDays() {
        return intParam("ai.auto_task.quiet_days", 7);
    }

    /** 知识库检索问句（默认用候选标题），供深度用知识库能力注入场景 prompt */
    protected String knowledgeQuestion(Candidate candidate) {
        return candidate.title();
    }

    /**
     * 扫描并生成待确认动作，返回本次新增动作数。
     *
     * @param orgId  租户组织ID
     * @param userId 触发用户（定时任务传 null，手动触发传当前登录用户）
     */
    public final int scan(String orgId, String userId) {
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId, userId);
        if (isBlocked(quota.getStatus())) {
            log.info("AI 员工自动任务扫描跳过：orgId={}，actionType={}，额度状态={}",
                    orgId, actionType(), quota.getStatus());
            return 0;
        }
        List<AgentModel> models = agentModelService.resolveTaskModels(orgId);
        if (models.isEmpty()) {
            log.info("AI 员工自动任务扫描跳过：orgId={} 未配置可用模型", orgId);
            return 0;
        }
        List<Candidate> candidates = resolveCandidates(orgId);
        if (candidates.isEmpty()) {
            return 0;
        }
        Set<String> pendingKeys = pendingKeys(orgId);
        Set<String> quietKeys = quietKeys(orgId);

        Map<String, Integer> createdByOwner = new HashMap<>();
        int created = 0;
        for (Candidate candidate : candidates) {
            String key = keyOf(candidate);
            if (pendingKeys.contains(key) || quietKeys.contains(key)) {
                continue;
            }
            try {
                String kbContext = buildKnowledgeContext(orgId, knowledgeQuestion(candidate));
                List<LlmMessage> messages = List.of(
                        new LlmMessage("system", systemPrompt()),
                        new LlmMessage("user", buildPrompt(orgId, candidate, kbContext)));
                String raw = callModel(orgId, userId, models, messages);
                String json = extractJson(raw);
                if (json == null) {
                    throw new IllegalStateException("模型返回内容非 JSON");
                }
                Map<String, Object> payload = parsePayload(json);
                insertAction(orgId, userId, candidate, payload);
                pendingKeys.add(key);
                if (StringUtils.isNotBlank(candidate.ownerId())) {
                    createdByOwner.merge(candidate.ownerId(), 1, Integer::sum);
                }
                created++;
            } catch (Exception e) {
                log.warn("AI 员工自动任务生成草稿失败，orgId={}，actionType={}，resourceId={}",
                        orgId, actionType(), candidate.resourceId(), e);
            }
        }
        createdByOwner.forEach((ownerId, count) -> notifyOwner(orgId, ownerId, count));
        return created;
    }

    /** 走任务模型候选链累积一整轮输出，全部失败抛异常（与 AiAgentTaskService.callModel 同口径） */
    protected String callModel(String orgId, String userId, List<AgentModel> models, List<LlmMessage> messages) {
        StringBuilder content = new StringBuilder();
        Exception lastError = null;
        for (AgentModel model : models) {
            try {
                aiQuotaService.checkModelDailyLimit(orgId, model, userId);
                LlmProvider provider = llmProviderFactory.get(model.getProvider());
                LlmChatRequest request = new LlmChatRequest();
                request.setModel(model.getModelName());
                request.setBaseUrl(model.getApiUrl());
                request.setApiKey(model.getApiKey());
                agentModelService.applyModelParams(request, model);
                request.setMessages(messages);

                LlmUsage usage = provider.chatStream(request, content::append);
                aiQuotaService.record(orgId, AiQuotaConstant.AI_EMPLOYEE, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return content.toString();
            } catch (Exception e) {
                lastError = e;
                if (content.length() > 0) {
                    log.error("AI 员工自动任务模型调用中途失败，actionType={}, provider={}, model={}",
                            actionType(), model.getProvider(), model.getModelName(), e);
                    throw new IllegalStateException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("AI 员工自动任务模型调用失败，自动降级，actionType={}, provider={}, model={}",
                        actionType(), model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("AI 员工自动任务全部模型调用失败，actionType={}，候选数={}", actionType(), models.size(), lastError);
        throw new IllegalStateException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    /** 从模型返回文本中提取首个 JSON 对象；无则返回 null */
    protected String extractJson(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end < start) {
            return null;
        }
        return raw.substring(start, end + 1);
    }

    /** 检索企业知识库并拼成带出处的参考文本；无命中或未配置时返回空串 */
    protected String buildKnowledgeContext(String orgId, String question) {
        if (StringUtils.isBlank(question)) {
            return "";
        }
        List<AiKnowledgeCitation> citations = aiKnowledgeService.retrieveCitations(orgId, question);
        if (citations.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < citations.size(); i++) {
            AiKnowledgeCitation citation = citations.get(i);
            sb.append(i + 1).append(". ");
            if (StringUtils.isNotBlank(citation.getDocName())) {
                sb.append('[').append(citation.getDocName()).append("] ");
            }
            sb.append(citation.getContent()).append('\n');
        }
        return sb.toString().trim();
    }

    private void insertAction(String orgId, String userId, Candidate candidate, Map<String, Object> payload) {
        long now = System.currentTimeMillis();
        AiEmployeeAction action = new AiEmployeeAction();
        action.setId(IDGenerator.nextStr());
        action.setOrganizationId(orgId);
        action.setActionType(actionType());
        action.setResourceType(candidate.resourceType());
        action.setResourceId(candidate.resourceId());
        action.setResourceName(candidate.resourceName());
        action.setCustomerId(candidate.customerId());
        action.setCustomerName(candidate.customerName());
        action.setOwnerId(candidate.ownerId());
        action.setPayload(JSON.toJSONString(payload));
        action.setStatus(AiEmployeeAction.STATUS_PENDING);
        action.setCreateUser(userId);
        action.setUpdateUser(userId);
        action.setCreateTime(now);
        action.setUpdateTime(now);
        actionMapper.insert(action);
    }

    /** PENDING 去重 key：actionType + resourceId（resource_id 为空的老数据回退 customer_id） */
    private Set<String> pendingKeys(String orgId) {
        return extAiEmployeeActionMapper.selectPending(orgId).stream()
                .map(this::keyOf)
                .collect(Collectors.toSet());
    }

    /** 静默期去重 key：最近被驳回的资源跳过，避免天天重复打扰 */
    private Set<String> quietKeys(String orgId) {
        long quietBefore = System.currentTimeMillis() - quietDays() * DAY_MILLIS;
        return extAiEmployeeActionMapper.selectRejectedRecent(orgId, quietBefore).stream()
                .map(this::keyOf)
                .collect(Collectors.toSet());
    }

    private String keyOf(AiEmployeeAction action) {
        String resourceId = StringUtils.isNotBlank(action.getResourceId())
                ? action.getResourceId() : action.getCustomerId();
        return action.getActionType() + ":" + resourceId;
    }

    private String keyOf(Candidate candidate) {
        return actionType() + ":" + candidate.resourceId();
    }

    /** 读整数系统参数，未配置或非法时回退默认值 */
    protected int intParam(String key, int defaultValue) {
        try {
            Parameter param = parameterMapper.selectByPrimaryKey(key);
            if (param != null && StringUtils.isNotBlank(param.getParamValue())) {
                int value = Integer.parseInt(param.getParamValue().trim());
                return value > 0 ? value : defaultValue;
            }
        } catch (Exception e) {
            log.warn("解析系统参数失败，key={}，使用默认值 {}", key, defaultValue, e);
        }
        return defaultValue;
    }

    protected boolean isBlocked(String status) {
        return AiQuotaConstant.STATUS_HARD_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status);
    }
}
