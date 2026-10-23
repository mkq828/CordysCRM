package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 城市经理绑定演示账号请求（demoUserId 为空=解绑）
 */
@Data
public class CityManagerDemoBindRequest {

    @Schema(description = "城市经理ID")
    private String managerId;

    @Schema(description = "演示账号用户ID(sys_user.id)，为空则解绑")
    private String demoUserId;
}
