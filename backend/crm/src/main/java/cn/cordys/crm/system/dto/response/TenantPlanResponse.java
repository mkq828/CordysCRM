package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租户套餐响应（含组织信息与管理员信息）
 */
@Data
public class TenantPlanResponse {

    @Schema(description = "套餐ID")
    private String id;

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "套餐版本(BASIC/PRO/ENTERPRISE)")
    private String version;

    @Schema(description = "状态(FREE/ACTIVE/EXPIRED)")
    private String status;

    @Schema(description = "到期时间(毫秒)")
    private Long expireTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "成交价(元)")
    private BigDecimal price;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "组织类型(PERSONAL/ENTERPRISE)")
    private String orgType;

    @Schema(description = "管理员手机号")
    private String phone;

    @Schema(description = "账号是否启用")
    private Boolean enabled;

    @Schema(description = "是否演示租户")
    private Boolean demo;

    @Schema(description = "签约城市经理ID")
    private String signManagerId;

    @Schema(description = "跟进城市经理ID")
    private String followManagerId;

    @Schema(description = "签约城市经理名")
    private String signManagerName;

    @Schema(description = "跟进城市经理名")
    private String followManagerName;

    @Schema(description = "最后一次登录时间")
    private Long lastLoginTime;

    @Schema(description = "剩余天数(负数表示已到期)")
    private Long remainingDays;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;
}
