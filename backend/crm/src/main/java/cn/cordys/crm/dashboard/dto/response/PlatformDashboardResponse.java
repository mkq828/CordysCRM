package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 平台大屏总览（仅 admin）。
 */
@Data
public class PlatformDashboardResponse {

    @Schema(description = "总租户数")
    private long totalTenant;

    @Schema(description = "企业租户数")
    private long enterpriseTenant;

    @Schema(description = "个人租户数")
    private long personalTenant;

    @Schema(description = "活跃租户数（近30天有登录）")
    private long activeTenant;

    @Schema(description = "总账号数")
    private long totalAccount;

    @Schema(description = "总线索数")
    private long totalClue;

    @Schema(description = "总客户数")
    private long totalCustomer;

    @Schema(description = "总商机数")
    private long totalOpportunity;

    @Schema(description = "总订单数")
    private long totalOrder;

    @Schema(description = "合同总额")
    private BigDecimal totalContractAmount;

    @Schema(description = "已回款")
    private BigDecimal totalReceivedAmount;

    @Schema(description = "未收款")
    private BigDecimal totalOutstandingAmount;

    @Schema(description = "各租户明细")
    private List<OrgOverviewRow> rows;
}
