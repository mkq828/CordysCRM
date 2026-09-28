package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 营收看板时间序列数据点
 */
@Data
public class PlatformRevenuePoint {

    @Schema(description = "时间桶(如 2026-W38 / 2026-09 / 2026)")
    private String bucket;

    @Schema(description = "合同金额(应收)")
    private BigDecimal contractAmount;

    @Schema(description = "实收金额(现金流)")
    private BigDecimal receivedAmount;

    @Schema(description = "开票金额(发票)")
    private BigDecimal invoiceAmount;
}
