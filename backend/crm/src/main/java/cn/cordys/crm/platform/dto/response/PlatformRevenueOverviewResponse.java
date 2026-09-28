package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 营收看板总览响应
 */
@Data
public class PlatformRevenueOverviewResponse {

    @Schema(description = "合同金额(应收)")
    private BigDecimal contractAmount;

    @Schema(description = "实收金额(现金流)")
    private BigDecimal receivedAmount;

    @Schema(description = "待收金额(合同金额-实收)")
    private BigDecimal outstandingAmount;

    @Schema(description = "开票金额(发票)")
    private BigDecimal invoiceAmount;

    @Schema(description = "时间序列")
    private List<PlatformRevenuePoint> series;
}
