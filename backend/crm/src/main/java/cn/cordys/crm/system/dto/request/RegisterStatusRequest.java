package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册审核状态查询请求
 */
@Data
public class RegisterStatusRequest {

    @NotBlank(message = "{register.phone.not_blank}")
    @Schema(description = "手机号")
    private String phone;
}
