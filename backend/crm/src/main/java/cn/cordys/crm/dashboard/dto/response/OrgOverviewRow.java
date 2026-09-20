package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单个租户的大屏概览行。
 */
@Data
public class OrgOverviewRow {

    @Schema(description = "租户ID")
    private String organizationId;

    @Schema(description = "租户名称")
    private String organizationName;

    @Schema(description = "租户类型(PERSONAL/ENTERPRISE)")
    private String orgType;

    @Schema(description = "账号数")
    private long accountCount;

    @Schema(description = "客户数")
    private long customerCount;

    @Schema(description = "商机数")
    private long opportunityCount;

    @Schema(description = "订单数")
    private long orderCount;

    @Schema(description = "合同总额")
    private BigDecimal contractAmount;

    @Schema(description = "已回款")
    private BigDecimal receivedAmount;

    @Schema(description = "未收款（合同总额 - 已回款）")
    private BigDecimal outstandingAmount;

    @Schema(description = "最后登录时间")
    private Long lastLoginTime;

    @Schema(description = "近30天是否有登录")
    private boolean active;

    @Schema(description = "连续使用天数（当前连续活跃天数）")
    private int usageDays;

    @Schema(description = "近30天活跃天数")
    private int activeDays30;

    @Schema(description = "最近登录城市")
    private String lastLoginCity;

    @Schema(description = "近30天主要登录城市")
    private String mainLoginCity;

    @Schema(description = "租户创建时间")
    private Long createTime;
}
