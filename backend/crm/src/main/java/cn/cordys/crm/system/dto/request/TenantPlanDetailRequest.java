package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 租户详情查询请求
 */
@Data
public class TenantPlanDetailRequest {

    @Schema(description = "租户组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "租户组织ID不能为空")
    private String organizationId;
}
