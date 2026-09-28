package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 租户套餐全局配置响应
 */
@Data
public class TenantPlanConfigResponse {

    @Schema(description = "免费试用天数")
    private Integer freeTrialDays;

    @Schema(description = "到期提醒天数(逗号分隔，如 30,7)")
    private String expireRemindDays;

    @Schema(description = "到期宽限天数(到期后仍可登录，仅横幅提示)")
    private Integer graceDays;
}
