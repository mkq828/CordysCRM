package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 付费租户演示标记请求
 */
@Data
public class TenantPlanDemoRequest {

    @Schema(description = "租户组织ID")
    @NotBlank(message = "组织ID不能为空")
    private String organizationId;

    @Schema(description = "是否演示租户")
    @NotNull(message = "请选择演示标记")
    private Boolean demo;
}
