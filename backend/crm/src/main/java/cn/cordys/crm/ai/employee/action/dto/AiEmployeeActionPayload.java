package cn.cordys.crm.ai.employee.action.dto;

import cn.cordys.common.util.JSON;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 待确认动作的载荷：LLM 生成的跟进话术草稿。以 JSON 串形式存入 {@code ai_employee_action.payload}，
 * 本类负责解析，供执行器与列表响应复用。
 */
@Data
public class AiEmployeeActionPayload {

    /** 跟进话术草稿（第一人称、口语化） */
    private String content;

    /** 跟进方式中文标签（电话 / 到访），执行时映射为存储编码 */
    private String followMethod;

    /** 补充建议（一句话） */
    private String suggestion;

    /** 解析载荷 JSON；为空或解析失败时返回空对象，由调用方兜底 */
    public static AiEmployeeActionPayload parse(String json) {
        AiEmployeeActionPayload payload = new AiEmployeeActionPayload();
        if (StringUtils.isBlank(json)) {
            return payload;
        }
        try {
            Map<String, Object> map = JSON.parseToMap(json);
            if (map != null) {
                payload.setContent(asString(map.get("content")));
                payload.setFollowMethod(asString(map.get("followMethod")));
                payload.setSuggestion(asString(map.get("suggestion")));
            }
        } catch (Exception ignored) {
            // 载荷解析失败时返回空对象，交由调用方兜底
        }
        return payload;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
