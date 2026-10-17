package cn.cordys.crm.ai.employee.action.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.dto.FillFieldCustomerRow;
import cn.cordys.crm.ai.employee.action.dto.FillFieldValueRow;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.PhoneField;
import cn.cordys.crm.system.dto.field.TextAreaField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自动补全数据扫描：扫「必填文本字段缺失」的客户 → 逐个走任务模型建议补全值 →
 * 落 PENDING 动作 → 站内信通知负责人。确认执行时（{@link AiAgentActionExecutor}）经权限复核后走
 * {@code customerService.update(agentInvoke=true)} 只补不删地落库。
 * <p>
 * 只针对必填且文本类（输入框/文本域/电话，与 {@code UpdateCustomerFieldTool.isTextLike} 白名单一致）的
 * 客户自定义字段；系统字段（含 businessKey，如客户名称）不纳入补全范围。
 * </p>
 */
@Service
@Slf4j
public class AiFillFieldScanService extends AbstractAiAutoScanService {

    private static final String OPERATION_AI_ACTION = "AI_EMPLOYEE_ACTION";

    private static final String SYSTEM_PROMPT = """
            你是企业 CRM 的 AI 销售助理，负责为客户的缺失字段补全合理的建议值（供人工确认后落库）。
            请根据给出的客户名称与缺失字段列表，输出一个 JSON 对象（不要任何其他文字、不要 Markdown 代码块），字段如下：
            {"fields":[{"fieldId":"字段ID","fieldName":"字段名","suggestedValue":"建议值","reason":"补全理由"}]}
            要求：
            1. 只补全「缺失字段」里列出的字段，fieldId、fieldName 必须原样照抄给出的值，不得编造或改动。
            2. suggestedValue 是给该客户的建议值，用简短中文填写；拿不准的字段宁可省略，也不要编造。
            3. reason 是一句简短理由，说明为什么这样建议。
            4. 全程使用中文业务表述，不得出现系统内部英文编码。
            """;

    @Resource
    private CustomerService customerService;

    @Resource
    private BaseMapper<Notification> notificationMapper;

    @Override
    public String actionType() {
        return AiEmployeeAction.ACTION_FILL_FIELD;
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected int scanLimit() {
        return intParam("ai.auto_task.fill_field.scan_limit", 10);
    }

    @Override
    protected List<Candidate> resolveCandidates(String orgId) {
        List<BaseField> requiredTextFields = requiredTextFields(orgId);
        if (requiredTextFields.isEmpty()) {
            return List.of();
        }
        List<String> fieldIds = requiredTextFields.stream().map(BaseField::getId).toList();
        List<FillFieldCustomerRow> customers = extAiEmployeeActionMapper.selectFillFieldCandidates(
                orgId, fieldIds, requiredTextFields.size(), scanLimit());
        if (customers.isEmpty()) {
            return List.of();
        }
        List<String> customerIds = customers.stream().map(FillFieldCustomerRow::getCustomerId).toList();
        List<FillFieldValueRow> existing = extAiEmployeeActionMapper.selectExistingFieldIds(orgId, customerIds, fieldIds);
        Map<String, Set<String>> existingByCustomer = new HashMap<>();
        for (FillFieldValueRow row : existing) {
            existingByCustomer.computeIfAbsent(row.getCustomerId(), k -> new HashSet<>()).add(row.getFieldId());
        }

        List<Candidate> candidates = new ArrayList<>();
        for (FillFieldCustomerRow customer : customers) {
            Set<String> existingIds = existingByCustomer.getOrDefault(customer.getCustomerId(), Set.of());
            List<BaseField> missing = requiredTextFields.stream()
                    .filter(f -> !existingIds.contains(f.getId()))
                    .toList();
            if (missing.isEmpty()) {
                continue;
            }
            Map<String, Object> extra = new HashMap<>();
            extra.put("missingFields", missing);
            candidates.add(new Candidate(AiEmployeeAction.RESOURCE_TYPE_CUSTOMER,
                    customer.getCustomerId(), customer.getCustomerName(),
                    customer.getCustomerId(), customer.getCustomerName(),
                    customer.getOwner(), customer.getCustomerName(), extra));
        }
        return candidates;
    }

    /** 取租户客户表单里「必填且文本类」的自定义字段（排除带 businessKey 的系统字段） */
    private List<BaseField> requiredTextFields(String orgId) {
        ModuleFormConfigDTO config = customerService.getFormConfig(orgId);
        if (config == null || config.getFields() == null) {
            return List.of();
        }
        return config.getFields().stream()
                .filter(this::isTextLike)
                .filter(BaseField::needRequireCheck)
                .filter(f -> !f.hasBusinessKey())
                .toList();
    }

    private boolean isTextLike(BaseField f) {
        return f instanceof InputField || f instanceof TextAreaField || f instanceof PhoneField;
    }

    @Override
    protected String buildPrompt(String orgId, Candidate candidate, String kbContext) {
        StringBuilder sb = new StringBuilder(buildContext(candidate));
        if (StringUtils.isNotBlank(kbContext)) {
            sb.append("\n\n## 参考资料（来自企业知识库）\n").append(kbContext);
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String buildContext(Candidate candidate) {
        List<BaseField> missing = (List<BaseField>) candidate.extra().get("missingFields");
        String missingText = missing == null ? "" : missing.stream()
                .map(f -> "字段ID：" + f.getId() + "，字段名：" + f.getName())
                .collect(Collectors.joining("；"));
        return "客户名称：" + candidate.resourceName() + "\n"
                + "缺失字段：" + (StringUtils.isBlank(missingText) ? "（无）" : missingText) + "\n"
                + "请为上述缺失字段给出补全建议。";
    }

    @Override
    protected Map<String, Object> parsePayload(String json) {
        Map<String, Object> map = JSON.parseToMap(json);
        if (map == null) {
            throw new IllegalStateException("模型返回内容非 JSON 对象");
        }
        Object fieldsObj = map.get("fields");
        if (!(fieldsObj instanceof List<?> fields)) {
            throw new IllegalStateException("模型返回缺少 fields 数组");
        }
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Object item : fields) {
            if (!(item instanceof Map<?, ?> m)) {
                continue;
            }
            String fieldId = str(m.get("fieldId"));
            String suggestedValue = str(m.get("suggestedValue"));
            if (StringUtils.isBlank(fieldId) || StringUtils.isBlank(suggestedValue)) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("fieldId", fieldId.trim());
            entry.put("fieldName", str(m.get("fieldName")));
            entry.put("suggestedValue", suggestedValue.trim());
            entry.put("reason", str(m.get("reason")));
            normalized.add(entry);
        }
        if (normalized.isEmpty()) {
            throw new IllegalStateException("模型返回没有可补全的字段");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("fields", normalized);
        return payload;
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @Override
    protected void notifyOwner(String orgId, String ownerId, int count) {
        long now = System.currentTimeMillis();
        String content = "您有 " + count + " 条 AI 补全数据建议待确认，请前往「AI 员工-待确认」查看并处理。";
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setReceiver(ownerId);
        notification.setSubject("AI 员工补全数据建议待确认");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOperator(ownerId);
        notification.setOperation(OPERATION_AI_ACTION);
        notification.setOrganizationId(orgId);
        notification.setResourceType(NotificationConstants.Module.SYSTEM);
        notification.setResourceId("ai_employee_action");
        notification.setResourceName("AI 补全数据建议");
        notification.setContent(content.getBytes(StandardCharsets.UTF_8));
        notification.setCreateUser(ownerId);
        notification.setUpdateUser(ownerId);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
    }
}
