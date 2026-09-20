package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 注册申请账号启用/禁用请求
 */
@Data
public class RegisterToggleRequest {

    @NotBlank(message = "{register.application.id.not_blank}")
    @Schema(description = "申请单ID")
    private String id;

    @NotNull(message = "{register.enabled.not_null}")
    @Schema(description = "是否启用")
    private Boolean enabled;
}
