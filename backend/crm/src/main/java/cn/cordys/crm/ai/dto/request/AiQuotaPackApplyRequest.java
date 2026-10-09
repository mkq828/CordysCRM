package cn.cordys.crm.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 租户申请购买额度包
 */
@Data
public class AiQuotaPackApplyRequest {

    @NotBlank(message = "额度包不能为空")
    @Schema(description = "额度包ID")
    private String packId;
}
