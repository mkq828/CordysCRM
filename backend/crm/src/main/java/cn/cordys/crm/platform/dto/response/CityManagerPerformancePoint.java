package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 城市经理业绩时间序列点（周/月/年桶）
 */
@Data
public class CityManagerPerformancePoint {

    @Schema(description = "时间桶(2025-W01 / 2025-01 / 2025)")
    private String bucket;

    @Schema(description = "签约客户数")
    private Long signedCount;

    @Schema(description = "合同金额(应收)")
    private BigDecimal contractAmount;

    @Schema(description = "回款金额(已核销)")
    private BigDecimal paymentAmount;
}
