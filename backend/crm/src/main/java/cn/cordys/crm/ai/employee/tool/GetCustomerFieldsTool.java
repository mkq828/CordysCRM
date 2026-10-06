package cn.cordys.crm.ai.employee.tool;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.customer.dto.response.CustomerGetResponse;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.PhoneField;
import cn.cordys.crm.system.dto.field.TextAreaField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只读工具：列出某客户可编辑的基础字段（客户名称 + 文本类表单字段，如电话/地址/备注）及当前值。
 * <p>
 * 供模型在提议修改前拿到准确的 fieldId 与当前值，从而生成精确的 old→new 建议；只开放文本类字段，
 * 负责人/多选/成员/附件等复杂字段不暴露，避免 AI 误改。
 * </p>
 */
@Service
public class GetCustomerFieldsTool implements AiAgentTool {

    @Resource
    private CustomerService customerService;

    @Override
    public String name() {
        return "get_customer_fields";
    }

    @Override
    public String description() {
        return "列某个客户可编辑的基础字段：客户名称 + 文本类表单字段（如电话/地址/备注），返回 fieldId、字段名、当前值。"
                + "参数 customerId=客户 id。修改客户信息前先调用本工具，拿到准确 fieldId 和当前值再提议修改。";
    }

    @Override
    public String execute(String orgId, String userId, Map<String, Object> args) {
        String customerId = str(args, "customerId");
        if (customerId == null || customerId.isBlank()) {
            return JSON.toJSONString(Map.of("error", "缺少 customerId 参数"));
        }
        CustomerGetResponse resp = customerService.getWithDataPermissionCheck(customerId, userId, orgId);

        Map<String, String> textFieldLabel = new LinkedHashMap<>();
        ModuleFormConfigDTO config = customerService.getFormConfig(orgId);
        if (config != null && config.getFields() != null) {
            for (BaseField f : config.getFields()) {
                if (isTextLike(f)) {
                    textFieldLabel.put(f.getId(), f.getName());
                }
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(field("name", "客户名称", resp.getName()));
        if (resp.getModuleFields() != null) {
            for (BaseModuleFieldValue v : resp.getModuleFields()) {
                String label = textFieldLabel.get(v.getFieldId());
                if (label == null) {
                    continue;
                }
                items.add(field(v.getFieldId(), label, scalar(v.getFieldValue())));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerId", customerId);
        result.put("customerName", resp.getName());
        result.put("fields", items);
        return JSON.toJSONString(result);
    }

    private boolean isTextLike(BaseField f) {
        return f instanceof InputField || f instanceof TextAreaField || f instanceof PhoneField;
    }

    private Map<String, Object> field(String fieldId, String label, String value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fieldId", fieldId);
        m.put("fieldLabel", label);
        m.put("value", value == null ? "" : value);
        return m;
    }

    private String scalar(Object v) {
        if (v instanceof String s) {
            return s;
        }
        return "";
    }

    private static String str(Map<String, Object> args, String key) {
        Object o = args == null ? null : args.get(key);
        return o == null ? null : String.valueOf(o);
    }
}
