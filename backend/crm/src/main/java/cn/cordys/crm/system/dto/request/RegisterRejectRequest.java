package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册申请驳回请求
 */
@Data
public class RegisterRejectRequest {

    @NotBlank(message = "{register.application.id.not_blank}")
    @Schema(description = "申请单ID")
    private String id;

    @NotBlank(message = "{register.remark.not_blank}")
    @Schema(description = "驳回备注")
    private String remark;
}
