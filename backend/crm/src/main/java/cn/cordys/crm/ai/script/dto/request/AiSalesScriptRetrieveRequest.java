package cn.cordys.crm.ai.script.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 销售话术检索请求：按客户场景检索并改写推荐话术。
 */
@Data
public class AiSalesScriptRetrieveRequest {

    @Schema(description = "客户场景描述")
    private String scenario;

    @Schema(description = "限定话术分类（可空）")
    private String category;

    @Schema(description = "返回条数，默认 3")
    private Integer topK;
}
