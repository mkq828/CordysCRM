package cn.cordys.crm.ai.callreview.dto.request;

import lombok.Data;

import java.util.Map;

/**
 * 保存回调配置：字段映射（标准字段 -> 供应商字段名）与启用开关。
 */
@Data
public class CallReviewConfigSaveRequest {

    /** 标准字段 -> 供应商字段名映射（key 见 CallReviewConstants.STANDARD_FIELDS） */
    private Map<String, String> fieldMapping;

    /** 是否启用回调 */
    private Boolean enable;
}
