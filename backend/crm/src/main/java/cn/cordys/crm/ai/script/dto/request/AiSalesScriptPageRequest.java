package cn.cordys.crm.ai.script.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 销售话术库分页请求（keyword 继承自 BaseCondition，category 按分类过滤）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiSalesScriptPageRequest extends BasePageRequest {

    @Schema(description = "话术分类")
    private String category;
}
