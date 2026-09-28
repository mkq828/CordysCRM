package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 租户套餐开通/续费请求
 */
@Data
public class TenantPlanOpenRequest {

    @Schema(description = "套餐ID")
    @NotBlank(message = "套餐ID不能为空")
    private String id;

    @Schema(description = "版本编码(BASIC/PRO/ENTERPRISE)")
    @NotBlank(message = "套餐版本不能为空")
    private String version;

    @Schema(description = "新的到期时间(毫秒)，为空时按版本有效期计算")
    private Long expireTime;

    @Schema(description = "备注")
    private String remark;
}
