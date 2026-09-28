package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 付费租户账号启用/禁用请求
 */
@Data
public class TenantPlanToggleRequest {

    @Schema(description = "租户组织ID")
    @NotBlank(message = "组织ID不能为空")
    private String organizationId;

    @Schema(description = "是否启用")
    @NotNull(message = "请选择启用/禁用")
    private Boolean enabled;
}
