package cn.cordys.crm.ai.employee.tool;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.customer.dto.response.CustomerGetResponse;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.PhoneField;
import cn.cordys.crm.system.dto.field.TextAreaField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 写建议工具：校验权限后生成「客户字段修改建议」，但**不落库**。结果带 {@code __proposal__} 标记，
 * 由 {@code AiAgentTaskService} 识别后经 {@code done.payload} 下发，用户确认后才走真正的更新接口。
 */
@Service
public class UpdateCustomerFieldTool implements AiAgentTool {

    @Resource
    private CustomerService customerService;

    @Resource
    private ResourcePermissionService resourcePermissionService;

    @Override
    public String name() {
        return "update_customer_field";
    }

    @Override
    public String description() {
        return "生成客户信息修改建议（只提议、不落库，需用户确认后才生效）。参数 customerId=客户 id，"
                + "changes=要改的字段数组，每项含 fieldId、fieldLabel、newValue（fieldId 取自 get_customer_fields）。"
                + "只能改客户名称或文本类表单字段，且必须是当前用户有编辑权限的客户。";
    }

    @Override
    public String execute(String orgId, String userId, Map<String, Object> args) {
        String customerId = str(args, "customerId");
        if (customerId == null || customerId.isBlank()) {
            return JSON.toJSONString(Map.of("error", "缺少 customerId 参数"));
        }
        List<Map<String, Object>> changes = parseChanges(args.get("changes"));
        if (changes.isEmpty()) {
            return JSON.toJSONString(Map.of("error", "缺少 changes 参数（要修改的字段）"));
        }

        // 服务端权限复核：角色位 + 行级数据权限（CustomerService.update 自身不查）
        resourcePermissionService.checkResourcePermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                customerId, FormKeyConstants.CUSTOMER, userId, orgId);

        CustomerGetResponse resp = customerService.get(customerId);
        if (resp == null) {
            return JSON.toJSONString(Map.of("error", "客户不存在"));
        }

        Map<String, String> current = new LinkedHashMap<>();
        current.put("name", resp.getName() == null ? "" : resp.getName());
        if (resp.getModuleFields() != null) {
            for (BaseModuleFieldValue v : resp.getModuleFields()) {
                current.put(v.getFieldId(), scalar(v.getFieldValue()));
            }
        }

        Map<String, String> label = new LinkedHashMap<>();
        label.put("name", "客户名称");
        ModuleFormConfigDTO config = customerService.getFormConfig(orgId);
        if (config != null && config.getFields() != null) {
            for (BaseField f : config.getFields()) {
                if (isTextLike(f)) {
                    label.put(f.getId(), f.getName());
                }
            }
        }

        List<Map<String, Object>> proposalChanges = new ArrayList<>();
        for (Map<String, Object> change : changes) {
            String fieldId = str(change, "fieldId");
            String newValue = str(change, "newValue");
            if (fieldId == null || fieldId.isBlank() || newValue == null) {
                continue;
            }
            String fieldLabel = label.get(fieldId);
            if (fieldLabel == null) {
                return JSON.toJSONString(Map.of("error", "字段 " + fieldId + " 不可通过对话修改"));
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fieldId", fieldId);
            item.put("fieldLabel", fieldLabel);
            item.put("oldValue", current.getOrDefault(fieldId, ""));
            item.put("newValue", newValue);
            proposalChanges.add(item);
        }
        if (proposalChanges.isEmpty()) {
            return JSON.toJSONString(Map.of("error", "没有可修改的有效字段"));
        }

        Map<String, Object> proposal = new LinkedHashMap<>();
        proposal.put("__proposal__", "customer_update");
        proposal.put("type", "customer_update");
        proposal.put("customerId", customerId);
        proposal.put("customerName", resp.getName());
        proposal.put("changes", proposalChanges);
        return JSON.toJSONString(proposal);
    }

    private boolean isTextLike(BaseField f) {
        return f instanceof InputField || f instanceof TextAreaField || f instanceof PhoneField;
    }

    private String scalar(Object v) {
        if (v instanceof String s) {
            return s;
        }
        return "";
    }

    /** 解析 changes：模型 JSON 里的数组经工具循环解析后可能是 JSON 字符串或已结构化列表 */
    private List<Map<String, Object>> parseChanges(Object changesObj) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (changesObj == null) {
            return result;
        }
        try {
            JsonNode node = changesObj instanceof String s
                    ? JSON.MAPPER.readTree(s)
                    : JSON.MAPPER.valueToTree(changesObj);
            if (node == null || !node.isArray()) {
                return result;
            }
            for (JsonNode c : node) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("fieldId", c.path("fieldId").asText());
                m.put("fieldLabel", c.path("fieldLabel").asText());
                m.put("newValue", c.path("newValue").asText());
                result.add(m);
            }
        } catch (Exception ignored) {
            // 解析失败按空处理
        }
        return result;
    }

    private static String str(Map<String, Object> args, String key) {
        Object o = args == null ? null : args.get(key);
        return o == null ? null : String.valueOf(o);
    }
}
