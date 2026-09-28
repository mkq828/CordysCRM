package cn.cordys.crm.platform.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 城市经理账号（平台员工档案，登录主体复用 sys_user，此处只存状态/快照）
 */
@Data
@Table(name = "platform_city_manager")
public class PlatformCityManager extends BaseModel {

    @Schema(description = "姓名(快照)")
    private String name;

    @Schema(description = "手机号(快照,登录标识)")
    private String phone;

    @Schema(description = "ENABLED在职/DISABLED离职")
    private String status;
}
