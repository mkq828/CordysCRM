package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 租户套餐升级请求
 */
@Data
public class TenantPlanUpgradeRequest {

    @Schema(description = "套餐ID")
    @NotBlank(message = "套餐ID不能为空")
    private String id;

    @Schema(description = "目标版本编码(BASIC/PRO/ENTERPRISE)")
    @NotBlank(message = "目标版本编码不能为空")
    private String editionCode;
}
