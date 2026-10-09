package cn.cordys.crm.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * admin 处理额度包订单（确认到账 / 取消）
 */
@Data
public class AiQuotaPackOrderHandleRequest {

    @NotBlank(message = "订单不能为空")
    @Schema(description = "订单ID")
    private String orderId;

    @Schema(description = "取消原因（取消时填写）")
    private String remark;
}
