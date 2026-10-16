package cn.cordys.crm.ai.briefing.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.briefing.domain.AiBriefing;
import cn.cordys.crm.ai.briefing.dto.response.BriefingResponse;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.dashboard.dto.response.AlertItem;
import cn.cordys.crm.dashboard.dto.response.BusinessScreenOverviewResponse;
import cn.cordys.crm.dashboard.service.AlertService;
import cn.cordys.crm.dashboard.service.BusinessScreenService;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 经营大屏「晨报/周报」：把大屏总览 + 主动预警拼成简报 prompt，用洞察型模型生成自然语言简报并落库。
 * 与 AI 经营助手同口径：免费全租户·不计量。定时任务生成仅落库（首屏卡片读取），手动生成再站内信告知操作人。
 */
@Service
@Slf4j
public class BriefingService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SYSTEM_PROMPT = "你是一名老板的私人经营助理。请根据给定的经营数据与预警信息，"
            + "撰写一份简洁的经营简报。请严格按以下两段式输出，"
            + "每段用 Markdown 编号列表（1. 2. 3.）分点，需要对比时用 Markdown 表格（| 指标 | 本期 | 上期 |），"
            + "用 **加粗** 突出关键数字，不要使用 # 标题：\n"
            + "现状：\n1. <核心经营指标与最需关注的异常>\n"
            + "建议：\n1. <可落地的行动建议>\n"
            + "控制在 500 字以内，用平实的老板口吻。";

    private static final String OPERATION_BRIEFING = "AI_BRIEFING_GENERATED";

    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private BusinessScreenService businessScreenService;
    @Resource
    private AlertService alertService;
    @Resource
    private BaseMapper<AiBriefing> aiBriefingMapper;
    @Resource
    private BaseMapper<Notification> notificationMapper;

    /**
     * 生成一份简报并落库。operatorId 为空表示定时任务触发（不单独发站内信），否则站内信告知操作人已生成。
     */
    public BriefingResponse generate(String orgId, String type, String operatorId) {
        String normalizedType = AiBriefing.TYPE_WEEKLY.equals(type) ? AiBriefing.TYPE_WEEKLY : AiBriefing.TYPE_DAILY;
        Map<String, Object> context = buildContext(orgId);

        String dataJson;
        try {
            dataJson = OBJECT_MAPPER.writeValueAsString(context);
        } catch (Exception e) {
            dataJson = "{}";
        }
        List<LlmMessage> messages = List.of(
                new LlmMessage("system", SYSTEM_PROMPT),
                new LlmMessage("user", "报告类型：" + (AiBriefing.TYPE_WEEKLY.equals(normalizedType) ? "周报" : "晨报")
                        + "\n\n经营数据与预警（JSON）：\n" + dataJson));

        String content = generateContent(orgId, messages);

        long now = System.currentTimeMillis();
        AiBriefing briefing = new AiBriefing();
        briefing.setId(IDGenerator.nextStr());
        briefing.setOrganizationId(orgId);
        briefing.setType(normalizedType);
        briefing.setPeriod(period(normalizedType));
        briefing.setContent(content);
        briefing.setCreateUser(operatorId);
        briefing.setUpdateUser(operatorId);
        briefing.setCreateTime(now);
        briefing.setUpdateTime(now);
        aiBriefingMapper.insert(briefing);

        if (StringUtils.isNotBlank(operatorId)) {
            notifyOperator(orgId, operatorId, normalizedType, content, briefing.getId(), now);
        }
        return toResponse(briefing);
    }

    /** 首屏简报卡读取最近一条（不限类型）。 */
    public BriefingResponse latest(String orgId) {
        List<AiBriefing> list = aiBriefingMapper.selectListByLambda(
                new LambdaQueryWrapper<AiBriefing>()
                        .eq(AiBriefing::getOrganizationId, orgId)
                        .orderByDesc(AiBriefing::getCreateTime));
        if (list == null || list.isEmpty()) {
            return null;
        }
        return toResponse(list.get(0));
    }

    /** 组装简报上下文：总览关键指标 + 转化漏斗 + 预警列表，避免 prompt 过长。 */
    private Map<String, Object> buildContext(String orgId) {
        BusinessScreenOverviewResponse overview = businessScreenService.overview(orgId, null);
        List<AlertItem> alerts = alertService.alerts(orgId).getItems();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("核心指标", overview.getKpi());
        context.put("转化漏斗", overview.getFunnel());
        context.put("合同阶段分布", overview.getContractStageDist());
        context.put("近12月合同趋势", overview.getContractTrend());
        context.put("主动预警", alerts == null ? List.of() : alerts);
        return context;
    }

    private String generateContent(String orgId, List<LlmMessage> messages) {
        List<AgentModel> models = agentModelService.resolveTaskModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }
        Exception lastError = null;
        for (AgentModel model : models) {
            try {
                return callModel(model, messages);
            } catch (Exception e) {
                lastError = e;
                log.warn("经营简报模型调用失败，降级尝试下一个，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("经营简报全部模型失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    /** 同步调用模型拿到完整文本（免费不计量，不做额度校验/记账）。 */
    private String callModel(AgentModel model, List<LlmMessage> messages) throws Exception {
        LlmProvider provider = llmProviderFactory.get(model.getProvider());
        LlmChatRequest llmRequest = new LlmChatRequest();
        llmRequest.setModel(model.getModelName());
        llmRequest.setBaseUrl(model.getApiUrl());
        llmRequest.setApiKey(model.getApiKey());
        agentModelService.applyModelParams(llmRequest, model);
        llmRequest.setMessages(messages);

        StringBuilder sb = new StringBuilder();
        provider.chatStream(llmRequest, sb::append);
        return sb.toString();
    }

    /** 晨报按天、周报按所在周的周一日期，作为周期标识。 */
    private String period(String type) {
        LocalDate today = LocalDate.now();
        if (AiBriefing.TYPE_WEEKLY.equals(type)) {
            return today.with(DayOfWeek.MONDAY).toString();
        }
        return today.toString();
    }

    private void notifyOperator(String orgId, String operatorId, String type, String content, String briefingId, long now) {
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setReceiver(operatorId);
        notification.setSubject(AiBriefing.TYPE_WEEKLY.equals(type) ? "本周经营周报已生成" : "今日经营晨报已生成");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOperator(operatorId);
        notification.setOperation(OPERATION_BRIEFING);
        notification.setOrganizationId(orgId);
        notification.setResourceType(NotificationConstants.Module.SYSTEM);
        notification.setResourceId(briefingId);
        notification.setResourceName(AiBriefing.TYPE_WEEKLY.equals(type) ? "经营周报" : "经营晨报");
        notification.setContent(content.getBytes(StandardCharsets.UTF_8));
        notification.setCreateUser(operatorId);
        notification.setUpdateUser(operatorId);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
    }

    private BriefingResponse toResponse(AiBriefing briefing) {
        BriefingResponse response = new BriefingResponse();
        response.setId(briefing.getId());
        response.setType(briefing.getType());
        response.setPeriod(briefing.getPeriod());
        response.setContent(briefing.getContent());
        response.setCreateTime(briefing.getCreateTime());
        return response;
    }
}
