package cn.cordys.crm.ai.employee.action.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.dto.ClueFlowRow;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 线索自动流转扫描：扫「感兴趣」且近期待跟进的线索 → 逐个走任务模型判断下一步阶段 →
 * 落 PENDING 动作 → 站内信通知负责人。确认执行时（{@link AiAgentActionExecutor}）经权限复核后流转阶段。
 * <p>
 * v1 只做「阶段流转」（感兴趣 → 成功/失败/跟进中），不做「转商机」——转商机需先转客户，链路重且风险高，留待后续。
 * </p>
 */
@Service
@Slf4j
public class AiClueFlowScanService extends AbstractAiAutoScanService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String OPERATION_AI_ACTION = "AI_EMPLOYEE_ACTION";

    /** 允许模型流转到的目标阶段（从「感兴趣」往前推进），阶段编码见 ClueStatus */
    static final Set<String> ALLOWED_TARGET_STAGES = Set.of("SUCCESS", "FAIL", "FOLLOWING");

    private static final String SYSTEM_PROMPT = """
            你是企业 CRM 的 AI 销售助理，负责判断「感兴趣」阶段的线索下一步该流转到哪个阶段。
            请根据线索信息输出一个 JSON 对象（不要任何其他文字、不要 Markdown 代码块），字段如下：
            {"targetStage":"SUCCESS","reason":"判断理由"}
            要求：
            1. targetStage 只能从以下三个值里选一个：
               - SUCCESS：线索已成交/明确签约，可流转到「成功」；
               - FAIL：线索已流失/明确拒绝，可流转到「失败」；
               - FOLLOWING：暂无法定论，还需继续跟进，可流转到「跟进中」。
            2. reason 是一句简短中文理由，说明为什么这样流转（可引用最近跟进时间、联系情况等）。
            3. 全程使用中文业务表述，不得出现系统内部英文编码。
            """;

    @Resource
    private BaseMapper<Notification> notificationMapper;

    @Override
    public String actionType() {
        return AiEmployeeAction.ACTION_CLUE_FLOW;
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected int scanLimit() {
        return intParam("ai.auto_task.clue_flow.scan_limit", 10);
    }

    @Override
    protected List<Candidate> resolveCandidates(String orgId) {
        int interestFollowDays = intParam("ai.auto_task.clue_flow.interest_follow_days", 7);
        long followAfter = System.currentTimeMillis() - interestFollowDays * DAY_MILLIS;
        List<ClueFlowRow> rows = extAiEmployeeActionMapper.selectClueFlowCandidates(
                orgId, followAfter, scanLimit());
        return rows.stream().map(this::toCandidate).toList();
    }

    private Candidate toCandidate(ClueFlowRow row) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("followTime", row.getFollowTime());
        extra.put("contact", row.getContact());
        extra.put("phone", row.getPhone());
        return new Candidate(AiEmployeeAction.RESOURCE_TYPE_CLUE, row.getClueId(), row.getClueName(),
                null, row.getClueName(), row.getOwner(), row.getClueName(), extra);
    }

    @Override
    protected String buildPrompt(String orgId, Candidate candidate, String kbContext) {
        StringBuilder sb = new StringBuilder(buildContext(candidate));
        if (StringUtils.isNotBlank(kbContext)) {
            sb.append("\n\n## 参考资料（来自企业知识库）\n").append(kbContext);
        }
        return sb.toString();
    }

    private String buildContext(Candidate candidate) {
        Long followTime = (Long) candidate.extra().get("followTime");
        String followText = followTime == null ? "未知"
                : Instant.ofEpochMilli(followTime).atZone(ZoneId.systemDefault()).toLocalDate().format(DAY_FMT);
        String contact = (String) candidate.extra().get("contact");
        String phone = (String) candidate.extra().get("phone");
        return "线索名称：" + candidate.resourceName() + "\n"
                + "当前阶段：感兴趣\n"
                + "最近跟进时间：" + followText + "\n"
                + "联系人：" + (StringUtils.isBlank(contact) ? "（无）" : contact) + "\n"
                + "联系电话：" + (StringUtils.isBlank(phone) ? "（无）" : phone) + "\n"
                + "请判断该线索下一步该流转到哪个阶段。";
    }

    @Override
    protected Map<String, Object> parsePayload(String json) {
        Map<String, Object> map = JSON.parseToMap(json);
        if (map == null) {
            throw new IllegalStateException("模型返回内容非 JSON 对象");
        }
        Object targetStage = map.get("targetStage");
        if (targetStage == null || !ALLOWED_TARGET_STAGES.contains(targetStage.toString().trim())) {
            throw new IllegalStateException("模型返回的目标阶段非法: " + targetStage);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetStage", targetStage.toString().trim());
        payload.put("reason", map.get("reason") == null ? null : map.get("reason").toString());
        return payload;
    }

    @Override
    protected void notifyOwner(String orgId, String ownerId, int count) {
        long now = System.currentTimeMillis();
        String content = "您有 " + count + " 条 AI 线索流转建议待确认，请前往「AI 员工-待确认」查看并处理。";
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setReceiver(ownerId);
        notification.setSubject("AI 员工线索流转建议待确认");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOperator(ownerId);
        notification.setOperation(OPERATION_AI_ACTION);
        notification.setOrganizationId(orgId);
        notification.setResourceType(NotificationConstants.Module.SYSTEM);
        notification.setResourceId("ai_employee_action");
        notification.setResourceName("AI 线索流转建议");
        notification.setContent(content.getBytes(StandardCharsets.UTF_8));
        notification.setCreateUser(ownerId);
        notification.setUpdateUser(ownerId);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
    }
}
