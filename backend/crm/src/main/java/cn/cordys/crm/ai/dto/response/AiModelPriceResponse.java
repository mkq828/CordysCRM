package cn.cordys.crm.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 模型单价
 */
@Data
public class AiModelPriceResponse {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "模型编码")
    private String modelCode;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "输入单价(元/1k token)")
    private BigDecimal inputPrice;

    @Schema(description = "输出单价(元/1k token)")
    private BigDecimal outputPrice;

    @Schema(description = "状态(1启用 0停用)")
    private Integer status;
}
