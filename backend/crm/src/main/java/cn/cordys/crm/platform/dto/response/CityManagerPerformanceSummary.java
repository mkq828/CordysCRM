package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 城市经理业绩汇总（单个经理）
 */
@Data
public class CityManagerPerformanceSummary {

    @Schema(description = "城市经理ID")
    private String managerId;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "签约客户数")
    private Long signedCount;

    @Schema(description = "合同金额(应收)")
    private BigDecimal contractAmount;

    @Schema(description = "回款金额(已核销)")
    private BigDecimal paymentAmount;
}
