package cn.cordys.crm.ai.employee.action.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.dto.AiEmployeeActionPayload;
import cn.cordys.crm.ai.employee.action.dto.StaleCustomerRow;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动跟进扫描：定时/手动扫「久未联系」客户 → 逐个走任务模型生成跟进话术草稿 → 落 PENDING 动作 → 站内信通知负责人。
 * 编排逻辑在 {@link AbstractAiAutoScanService}，本类只实现跟进场景的候选查询、提示词、载荷解析与通知。
 */
@Service
@Slf4j
public class AiFollowUpScanService extends AbstractAiAutoScanService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String OPERATION_AI_ACTION = "AI_EMPLOYEE_ACTION";

    private static final String SYSTEM_PROMPT = """
            你是企业 CRM 的 AI 销售助理，负责为「久未联系」的客户起草一条跟进建议，供销售确认后写入跟进记录。
            请根据客户信息输出一个 JSON 对象（不要任何其他文字、不要 Markdown 代码块），字段如下：
            {"content":"跟进话术草稿","followMethod":"电话","suggestion":"补充建议"}
            要求：
            1. content 是第一人称、口语化的跟进内容，贴合客户实际情况，80~150 字，直接可用于跟进记录。
            2. followMethod 只能是「电话」「微信」或「到访」三选一。
            3. suggestion 是一句简短补充建议（如提醒要点、切入话题），可为空。
            4. 全程使用中文业务表述，不得出现系统内部英文编码。
            """;

    @Resource
    private BaseMapper<Notification> notificationMapper;

    @Override
    public String actionType() {
        return AiEmployeeAction.ACTION_FOLLOW_UP_CREATE;
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected int scanLimit() {
        return intParam("ai.auto_task.follow_up.scan_limit", 10);
    }

    @Override
    protected int quietDays() {
        return intParam("ai.auto_task.follow_up.reject_quiet_days", 7);
    }

    @Override
    protected List<Candidate> resolveCandidates(String orgId) {
        int staleDays = intParam("ai.auto_task.follow_up.stale_days", 30);
        long staleBefore = System.currentTimeMillis() - staleDays * DAY_MILLIS;
        List<StaleCustomerRow> rows = extAiEmployeeActionMapper.selectStaleCustomers(orgId, staleBefore, scanLimit());
        return rows.stream().map(this::toCandidate).toList();
    }

    private Candidate toCandidate(StaleCustomerRow row) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("followTime", row.getFollowTime());
        extra.put("contractAmount", row.getContractAmount());
        return new Candidate(AiEmployeeAction.RESOURCE_TYPE_CUSTOMER, row.getCustomerId(), row.getCustomerName(),
                row.getCustomerId(), row.getCustomerName(), row.getOwner(), row.getCustomerName(), extra);
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
        BigDecimal amount = (BigDecimal) candidate.extra().get("contractAmount");
        String followText = followTime == null ? "从未跟进"
                : Instant.ofEpochMilli(followTime).atZone(ZoneId.systemDefault()).toLocalDate().format(DAY_FMT);
        String amountText = amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
        return "客户名称：" + candidate.customerName() + "\n"
                + "最近跟进时间：" + followText + "\n"
                + "累计合同额：" + amountText + " 元\n"
                + "请为该客户起草一条跟进建议。";
    }

    @Override
    protected Map<String, Object> parsePayload(String json) {
        AiEmployeeActionPayload payload = AiEmployeeActionPayload.parse(json);
        if (StringUtils.isBlank(payload.getContent())) {
            throw new IllegalStateException("模型返回的跟进话术为空");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("content", payload.getContent());
        map.put("followMethod", payload.getFollowMethod());
        map.put("suggestion", payload.getSuggestion());
        return map;
    }

    @Override
    protected void notifyOwner(String orgId, String ownerId, int count) {
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
}
