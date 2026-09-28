package cn.cordys.crm.ai.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 模型单价（成本看板折算用，admin 手动维护）
 */
@Data
@Table(name = "ai_model_price")
public class AiModelPrice extends BaseModel {

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
