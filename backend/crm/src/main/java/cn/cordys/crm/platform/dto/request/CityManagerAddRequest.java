package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 城市经理新增请求
 */
@Data
public class CityManagerAddRequest {

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号(登录标识)")
    private String phone;

    @Schema(description = "登录密码(不填默认123456)")
    private String password;
}
