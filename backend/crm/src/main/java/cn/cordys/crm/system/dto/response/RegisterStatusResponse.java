package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 注册审核状态响应
 */
@Data
public class RegisterStatusResponse {

    @Schema(description = "注册类型(PERSONAL/ENTERPRISE)")
    private String type;

    @Schema(description = "审核状态(PENDING/APPROVED/REJECTED)")
    private String verifyStatus;

    @Schema(description = "审核备注")
    private String verifyRemark;
}
