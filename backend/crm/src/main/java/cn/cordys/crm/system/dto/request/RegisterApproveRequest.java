package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册申请审核通过请求
 */
@Data
public class RegisterApproveRequest {

    @NotBlank(message = "{register.application.id.not_blank}")
    @Schema(description = "申请单ID")
    private String id;
}
