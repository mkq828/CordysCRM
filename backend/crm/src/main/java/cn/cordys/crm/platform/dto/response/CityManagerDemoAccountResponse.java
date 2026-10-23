package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 演示账号下拉项（含当前绑定人，供 admin 分配）
 */
@Data
public class CityManagerDemoAccountResponse {

    @Schema(description = "演示账号用户ID(sys_user.id)")
    private String id;

    @Schema(description = "演示账号姓名")
    private String name;

    @Schema(description = "演示账号手机号")
    private String phone;

    @Schema(description = "当前绑定的城市经理ID（未绑定为空）")
    private String boundManagerId;

    @Schema(description = "当前绑定的城市经理姓名")
    private String boundManagerName;
}
