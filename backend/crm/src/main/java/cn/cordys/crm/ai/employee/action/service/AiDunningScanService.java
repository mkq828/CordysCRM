package cn.cordys.crm.ai.employee.action.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.dto.DunningRow;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动催款扫描：扫「即将到期/已逾期」的合同与回款计划 → 逐个走任务模型生成催款话术草稿 →
 * 落 PENDING 动作 → 站内信通知负责人。确认执行时（{@link AiAgentActionExecutor}）再把话术作为站内信发给负责人。
 */
@Service
@Slf4j
public class AiDunningScanService extends AbstractAiAutoScanService {

    private static final String OPERATION_AI_ACTION = "AI_EMPLOYEE_ACTION";

    private static final String SYSTEM_PROMPT = """
            你是企业 CRM 的 AI 销售助理，负责为「即将到期/已逾期」的合同或回款计划起草一条催款话术，供负责人确认后使用。
            请根据资源信息输出一个 JSON 对象（不要任何其他文字、不要 Markdown 代码块），字段如下：
            {"dunningContent":"催款话术","suggestion":"补充建议"}
            要求：
            1. dunningContent 是第一人称、口语化的催款话术，语气得体、不冒犯，可提及到期时间与金额，80~150 字，直接可用于联系客户催款。
            2. 不得编造金额、日期或客户信息，只能依据给出的信息。
            3. suggestion 是一句简短补充建议（如风险提示、切入话术），可为空。
            4. 全程使用中文业务表述，不得出现系统内部英文编码。
            """;

    @Resource
    private BaseMapper<Notification> notificationMapper;

    @Override
    public String actionType() {
        return AiEmployeeAction.ACTION_DUNNING;
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected int scanLimit() {
        return intParam("ai.auto_task.dunning.scan_limit", 10);
    }

    @Override
    protected List<Candidate> resolveCandidates(String orgId) {
        int lookaheadDays = intParam("ai.auto_task.dunning.lookahead_days", 7);
        int overdueDays = intParam("ai.auto_task.dunning.overdue_days", 30);
        long now = System.currentTimeMillis();
        long upper = now + lookaheadDays * DAY_MILLIS;
        long lower = now - overdueDays * DAY_MILLIS;
        List<DunningRow> rows = extAiEmployeeActionMapper.selectDunningCandidates(orgId, lower, upper, scanLimit());
        return rows.stream().map(this::toCandidate).toList();
    }

    private Candidate toCandidate(DunningRow row) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("dueTime", row.getDueTime());
        extra.put("amount", row.getAmount());
        extra.put("resourceType", row.getResourceType());
        return new Candidate(row.getResourceType(), row.getResourceId(), row.getResourceName(),
                row.getCustomerId(), row.getCustomerName(), row.getOwner(), row.getResourceName(), extra);
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
        Long dueTime = (Long) candidate.extra().get("dueTime");
        BigDecimal amount = (BigDecimal) candidate.extra().get("amount");
        String typeText = AiEmployeeAction.RESOURCE_TYPE_PAYMENT_PLAN.equals(candidate.resourceType()) ? "回款计划" : "合同";
        long now = System.currentTimeMillis();
        long days = dueTime == null ? 0 : (dueTime - now) / DAY_MILLIS;
        String dueText = days >= 0 ? "还有 " + days + " 天到期" : "已逾期 " + (-days) + " 天";
        String amountText = amount == null ? "未知" : amount.stripTrailingZeros().toPlainString();
        return "资源类型：" + typeText + "\n"
                + "资源名称：" + candidate.resourceName() + "\n"
                + "客户名称：" + candidate.customerName() + "\n"
                + "到期情况：" + dueText + "\n"
                + "金额：" + amountText + " 元\n"
                + "请为该资源起草一条催款话术。";
    }

    @Override
    protected Map<String, Object> parsePayload(String json) {
        Map<String, Object> map = JSON.parseToMap(json);
        if (map == null) {
            throw new IllegalStateException("模型返回内容非 JSON 对象");
        }
        Object content = map.get("dunningContent");
        if (content == null || StringUtils.isBlank(content.toString())) {
            throw new IllegalStateException("模型返回的催款话术为空");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("dunningContent", content.toString());
        payload.put("suggestion", map.get("suggestion") == null ? null : map.get("suggestion").toString());
        return payload;
    }

    @Override
    protected void notifyOwner(String orgId, String ownerId, int count) {
        long now = System.currentTimeMillis();
        String content = "您有 " + count + " 条 AI 催款建议待确认，请前往「AI 员工-待确认」查看并处理。";
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setReceiver(ownerId);
        notification.setSubject("AI 员工催款建议待确认");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOperator(ownerId);
        notification.setOperation(OPERATION_AI_ACTION);
        notification.setOrganizationId(orgId);
        notification.setResourceType(NotificationConstants.Module.SYSTEM);
        notification.setResourceId("ai_employee_action");
        notification.setResourceName("AI 催款建议");
        notification.setContent(content.getBytes(StandardCharsets.UTF_8));
        notification.setCreateUser(ownerId);
        notification.setUpdateUser(ownerId);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
    }
}
