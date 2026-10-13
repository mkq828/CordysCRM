package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 区域经营指标（经营大屏下钻：按省级行政区划）。
 */
@Data
public class RegionKpi {

    @Schema(description = "客户数")
    private long customerCount;

    @Schema(description = "合同金额合计（不含作废）")
    private BigDecimal contractAmount;

    @Schema(description = "已核销回款")
    private BigDecimal verifiedAmount;

    @Schema(description = "待收金额（合同金额-已核销回款）")
    private BigDecimal pendingAmount;

    @Schema(description = "合同数（不含作废）")
    private long contractCount;
}
