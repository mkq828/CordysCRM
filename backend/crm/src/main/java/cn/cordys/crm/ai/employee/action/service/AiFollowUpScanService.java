package cn.cordys.crm.ai.employee.action.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.dto.AiEmployeeActionPayload;
import cn.cordys.crm.ai.employee.action.dto.StaleCustomerRow;
import cn.cordys.crm.ai.employee.action.mapper.ExtAiEmployeeActionMapper;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自动跟进扫描：定时/手动扫「久未联系」客户 → 逐个走任务模型生成跟进话术草稿 → 落 PENDING 动作 → 站内信通知负责人。
 * <p>
 * 无登录态（定时任务）时 userId 传 null，额度按租户口径计量；手动触发时以当前登录用户计量。
 * </p>
 */
@Service
@Slf4j
public class AiFollowUpScanService {

    /** 久未联系判定阈值：最新跟进时间早于 now-30 天（或从未跟进） */
    private static final int STALE_DAYS = 30;
    private static final long DAY_MILLIS = 86_400_000L;

    /** 每次扫描最多生成的动作数，防一次扫太多烧额度 */
    private static final int SCAN_LIMIT = 10;

    /** 驳回静默天数：驳回后该客户在静默期内不再重复生成跟进建议 */
    private static final int REJECT_QUIET_DAYS = 7;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String OPERATION_AI_ACTION = "AI_EMPLOYEE_ACTION";

    private static final String SYSTEM_PROMPT = """
            你是企业 CRM 的 AI 销售助理，负责为「久未联系」的客户起草一条跟进建议，供销售确认后写入跟进记录。
            请根据客户信息输出一个 JSON 对象（不要任何其他文字、不要 Markdown 代码块），字段如下：
            {"content":"跟进话术草稿","followMethod":"电话","suggestion":"补充建议"}
            要求：
            1. content 是第一人称、口语化的跟进内容，贴合客户实际情况，80~150 字，直接可用于跟进记录。
            2. followMethod 只能是「电话」或「到访」二选一。
            3. suggestion 是一句简短补充建议（如提醒要点、切入话题），可为空。
            4. 全程使用中文业务表述，不得出现系统内部英文编码。
            """;

    @Resource
    private ExtAiEmployeeActionMapper extAiEmployeeActionMapper;

    @Resource
    private BaseMapper<AiEmployeeAction> actionMapper;

    @Resource
    private BaseMapper<Notification> notificationMapper;

    @Resource
    private AgentModelService agentModelService;

    @Resource
    private AiQuotaService aiQuotaService;

    @Resource
    private LlmProviderFactory llmProviderFactory;

    /**
     * 扫描并生成待确认动作，返回本次新增动作数。
     *
     * @param orgId  租户组织ID
     * @param userId 触发用户（定时任务传 null，手动触发传当前登录用户）
     */
    public int scan(String orgId, String userId) {
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId, userId);
        if (isBlocked(quota.getStatus())) {
            log.info("AI 员工跟进扫描跳过：orgId={}，额度状态={}", orgId, quota.getStatus());
            return 0;
        }
        List<AgentModel> models = agentModelService.resolveTaskModels(orgId);
        if (models.isEmpty()) {
            log.info("AI 员工跟进扫描跳过：orgId={} 未配置可用模型", orgId);
            return 0;
        }
        long staleBefore = System.currentTimeMillis() - STALE_DAYS * DAY_MILLIS;
        List<StaleCustomerRow> customers = extAiEmployeeActionMapper.selectStaleCustomers(orgId, staleBefore, SCAN_LIMIT);
        if (customers.isEmpty()) {
            return 0;
        }
        Set<String> pendingCustomerIds = extAiEmployeeActionMapper.selectPending(orgId).stream()
                .map(AiEmployeeAction::getCustomerId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        long quietBefore = System.currentTimeMillis() - REJECT_QUIET_DAYS * DAY_MILLIS;
        Set<String> quietCustomerIds = new HashSet<>(extAiEmployeeActionMapper.selectQuietCustomers(orgId, quietBefore));

        Map<String, Integer> createdByOwner = new HashMap<>();
        int created = 0;
        for (StaleCustomerRow row : customers) {
            if (pendingCustomerIds.contains(row.getCustomerId()) || quietCustomerIds.contains(row.getCustomerId())) {
                continue;
            }
            try {
                AiEmployeeActionPayload draft = generateDraft(orgId, userId, models, row);
                insertAction(orgId, userId, row, draft);
                pendingCustomerIds.add(row.getCustomerId());
                createdByOwner.merge(row.getOwner(), 1, Integer::sum);
                created++;
            } catch (Exception e) {
                log.warn("AI 员工生成跟进草稿失败，orgId={}，customerId={}", orgId, row.getCustomerId(), e);
            }
        }
        createdByOwner.forEach((ownerId, count) -> notifyOwner(orgId, ownerId, count));
        return created;
    }

    private AiEmployeeActionPayload generateDraft(String orgId, String userId, List<AgentModel> models,
                                                  StaleCustomerRow row) {
        List<LlmMessage> messages = List.of(
                new LlmMessage("system", SYSTEM_PROMPT),
                new LlmMessage("user", buildContext(row)));
        String raw = callModel(orgId, userId, models, messages);
        String json = extractJson(raw);
        if (json == null) {
            throw new IllegalStateException("模型返回内容非 JSON");
        }
        AiEmployeeActionPayload payload = AiEmployeeActionPayload.parse(json);
        if (StringUtils.isBlank(payload.getContent())) {
            throw new IllegalStateException("模型返回的跟进话术为空");
        }
        return payload;
    }

    /** 走任务模型候选链累积一整轮输出，全部失败抛异常（与 AiAgentTaskService.callModel 同口径） */
    private String callModel(String orgId, String userId, List<AgentModel> models, List<LlmMessage> messages) {
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
                    log.error("AI 员工跟进草稿模型调用中途失败，provider={}, model={}",
                            model.getProvider(), model.getModelName(), e);
                    throw new IllegalStateException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("AI 员工跟进草稿模型调用失败，自动降级，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("AI 员工跟进草稿全部模型调用失败，候选数={}", models.size(), lastError);
        throw new IllegalStateException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    private String buildContext(StaleCustomerRow row) {
        String followText = row.getFollowTime() == null ? "从未跟进"
                : Instant.ofEpochMilli(row.getFollowTime()).atZone(ZoneId.systemDefault()).toLocalDate().format(DAY_FMT);
        String amountText = row.getContractAmount() == null ? "0"
                : row.getContractAmount().stripTrailingZeros().toPlainString();
        return "客户名称：" + row.getCustomerName() + "\n"
                + "最近跟进时间：" + followText + "\n"
                + "累计合同额：" + amountText + " 元\n"
                + "请为该客户起草一条跟进建议。";
    }

    private String extractJson(String raw) {
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

    private void insertAction(String orgId, String userId, StaleCustomerRow row, AiEmployeeActionPayload draft) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", draft.getContent());
        payload.put("followMethod", draft.getFollowMethod());
        payload.put("suggestion", draft.getSuggestion());
        long now = System.currentTimeMillis();
        AiEmployeeAction action = new AiEmployeeAction();
        action.setId(IDGenerator.nextStr());
        action.setOrganizationId(orgId);
        action.setActionType(AiEmployeeAction.ACTION_FOLLOW_UP_CREATE);
        action.setCustomerId(row.getCustomerId());
        action.setCustomerName(row.getCustomerName());
        action.setOwnerId(row.getOwner());
        action.setPayload(JSON.toJSONString(payload));
        action.setStatus(AiEmployeeAction.STATUS_PENDING);
        action.setCreateUser(userId);
        action.setUpdateUser(userId);
        action.setCreateTime(now);
        action.setUpdateTime(now);
        actionMapper.insert(action);
    }

    private void notifyOwner(String orgId, String ownerId, int count) {
        long now = System.currentTimeMillis();
        String content = "您有 " + count + " 条 AI 跟进建议待确认，请前往「AI 员工-待确认」查看并处理。";
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setReceiver(ownerId);
        notification.setSubject("AI 员工跟进建议待确认");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOperator(ownerId);
        notification.setOperation(OPERATION_AI_ACTION);
        notification.setOrganizationId(orgId);
        notification.setResourceType(NotificationConstants.Module.SYSTEM);
        notification.setResourceId("ai_employee_action");
        notification.setResourceName("AI 跟进建议");
        notification.setContent(content.getBytes(StandardCharsets.UTF_8));
        notification.setCreateUser(ownerId);
        notification.setUpdateUser(ownerId);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
    }

    private boolean isBlocked(String status) {
        return AiQuotaConstant.STATUS_HARD_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status);
    }
}
