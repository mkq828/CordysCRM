package cn.cordys.crm.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 额度包商品保存（id 为空时新增）
 */
@Data
public class AiQuotaPackSaveRequest {

    @Schema(description = "主键，为空时新增")
    private String id;

    @Schema(description = "AI 功能编码，空串=全功能通用包")
    private String featureCode;

    @NotBlank(message = "额度包名称不能为空")
    @Schema(description = "额度包名称")
    private String name;

    @NotNull(message = "次数不能为空")
    @Schema(description = "次数")
    private Integer calls;

    @NotNull(message = "价格不能为空")
    @Schema(description = "价格(分)")
    private Long priceFen;

    @Schema(description = "状态:1启用 0停用")
    private Integer status;

    @Schema(description = "排序，越小越靠前")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
