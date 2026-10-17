package cn.cordys.crm.ai.employee.action.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.dto.StaleCustomerRow;
import cn.cordys.crm.customer.tag.domain.CustomerTag;
import cn.cordys.crm.customer.tag.service.CustomerTagService;
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
import java.util.stream.Collectors;

/**
 * 自动打标签扫描：扫「久未联系」客户 → 逐个走任务模型建议标签 → 落 PENDING 动作 → 站内信通知负责人。
 * 确认执行时（{@link AiAgentActionExecutor}）经权限复核后把标签落到客户（标签不存在则按名创建）。
 */
@Service
@Slf4j
public class AiTagScanService extends AbstractAiAutoScanService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String OPERATION_AI_ACTION = "AI_EMPLOYEE_ACTION";

    private static final String SYSTEM_PROMPT = """
            你是企业 CRM 的 AI 销售助理，负责为「久未联系」的客户打上一个合适的标签，供精准营销。
            请根据客户信息输出一个 JSON 对象（不要任何其他文字、不要 Markdown 代码块），字段如下：
            {"tagName":"标签名","reason":"打标签理由","suggestedTags":["备选标签1","备选标签2"]}
            要求：
            1. tagName 是给该客户打的标签，优先从给出的「已有标签」里选一个最贴切的；没有合适的就写一个简短中文标签（2~6 字），如「流失预警」「高价值」「待跟进」。
            2. reason 是一句简短理由，说明为什么打这个标签。
            3. suggestedTags 是 1~2 个备选标签（字符串数组），可为空。
            4. 全程使用中文业务表述，不得出现系统内部英文编码。
            """;

    @Resource
    private BaseMapper<Notification> notificationMapper;

    @Resource
    private CustomerTagService customerTagService;

    @Override
    public String actionType() {
        return AiEmployeeAction.ACTION_TAG;
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected int scanLimit() {
        return intParam("ai.auto_task.tag.scan_limit", 10);
    }

    @Override
    protected List<Candidate> resolveCandidates(String orgId) {
        int churnDays = intParam("ai.auto_task.tag.churn_days", 30);
        long churnBefore = System.currentTimeMillis() - churnDays * DAY_MILLIS;
        List<StaleCustomerRow> rows = extAiEmployeeActionMapper.selectChurnCustomers(
                orgId, churnBefore, CustomerTagService.CHURN_TAG_NAME, scanLimit());
        return rows.stream().map(this::toCandidate).toList();
    }

    private Candidate toCandidate(StaleCustomerRow row) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("followTime", row.getFollowTime());
        return new Candidate(AiEmployeeAction.RESOURCE_TYPE_CUSTOMER, row.getCustomerId(), row.getCustomerName(),
                row.getCustomerId(), row.getCustomerName(), row.getOwner(), row.getCustomerName(), extra);
    }

    @Override
    protected String buildPrompt(String orgId, Candidate candidate, String kbContext) {
        StringBuilder sb = new StringBuilder(buildContext(orgId, candidate));
        if (StringUtils.isNotBlank(kbContext)) {
            sb.append("\n\n## 参考资料（来自企业知识库）\n").append(kbContext);
        }
        return sb.toString();
    }

    private String buildContext(String orgId, Candidate candidate) {
        Long followTime = (Long) candidate.extra().get("followTime");
        String followText = followTime == null ? "从未跟进"
                : Instant.ofEpochMilli(followTime).atZone(ZoneId.systemDefault()).toLocalDate().format(DAY_FMT);
        String existingTags = customerTagService.list(orgId).stream()
                .map(CustomerTag::getName)
                .collect(Collectors.joining("、"));
        return "客户名称：" + candidate.customerName() + "\n"
                + "最近跟进时间：" + followText + "\n"
                + "已有标签：" + (StringUtils.isBlank(existingTags) ? "（无）" : existingTags) + "\n"
                + "请为该客户打一个合适的标签。";
    }

    @Override
    protected Map<String, Object> parsePayload(String json) {
        Map<String, Object> map = JSON.parseToMap(json);
        if (map == null) {
            throw new IllegalStateException("模型返回内容非 JSON 对象");
        }
        Object tagName = map.get("tagName");
        if (tagName == null || StringUtils.isBlank(tagName.toString())) {
            throw new IllegalStateException("模型返回的标签名为空");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("tagName", tagName.toString().trim());
        payload.put("reason", map.get("reason") == null ? null : map.get("reason").toString());
        payload.put("suggestedTags", map.get("suggestedTags"));
        return payload;
    }

    @Override
    protected void notifyOwner(String orgId, String ownerId, int count) {
        long now = System.currentTimeMillis();
        String content = "您有 " + count + " 条 AI 打标签建议待确认，请前往「AI 员工-待确认」查看并处理。";
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setReceiver(ownerId);
        notification.setSubject("AI 员工打标签建议待确认");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOperator(ownerId);
        notification.setOperation(OPERATION_AI_ACTION);
        notification.setOrganizationId(orgId);
        notification.setResourceType(NotificationConstants.Module.SYSTEM);
        notification.setResourceId("ai_employee_action");
        notification.setResourceName("AI 打标签建议");
        notification.setContent(content.getBytes(StandardCharsets.UTF_8));
        notification.setCreateUser(ownerId);
        notification.setUpdateUser(ownerId);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
    }
}
