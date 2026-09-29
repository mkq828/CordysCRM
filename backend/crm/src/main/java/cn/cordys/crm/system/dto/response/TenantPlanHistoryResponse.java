package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租户套餐历史记录条目
 */
@Data
public class TenantPlanHistoryResponse {

    @Schema(description = "动作(OPEN=开通/续费 UPGRADE=升级)")
    private String action;

    @Schema(description = "变更前版本")
    private String fromVersion;

    @Schema(description = "变更后版本")
    private String toVersion;

    @Schema(description = "本次成交价(元)")
    private BigDecimal price;

    @Schema(description = "本次后到期时间(毫秒)")
    private Long expireTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "操作时间(毫秒)")
    private Long createTime;

    @Schema(description = "操作人")
    private String createUser;
}
