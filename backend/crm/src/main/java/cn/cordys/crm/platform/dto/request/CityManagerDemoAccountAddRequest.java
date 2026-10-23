package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 新增演示账号请求（演示账号=演示租户下的租户管理员账号）
 */
@Data
public class CityManagerDemoAccountAddRequest {

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号(登录标识)")
    private String phone;

    @Schema(description = "登录密码(不填默认123456)")
    private String password;
}
