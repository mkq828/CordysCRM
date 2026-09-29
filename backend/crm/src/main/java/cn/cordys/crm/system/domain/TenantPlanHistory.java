package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租户套餐开通/续费/升级历史（付费用户详情页展示成交价与版本变更轨迹）
 */
@Data
@Table(name = "tenant_plan_history")
public class TenantPlanHistory extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

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
}
