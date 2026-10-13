package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 经营大屏核心指标。
 */
@Data
public class BusinessScreenKpi {

    @Schema(description = "客户总数")
    private long customerCount;

    @Schema(description = "线索总数（未转化）")
    private long clueCount;

    @Schema(description = "商机数")
    private long opportunityCount;

    @Schema(description = "订单数")
    private long orderCount;

    @Schema(description = "合同数（不含作废）")
    private long contractCount;

    @Schema(description = "合同金额合计（不含作废）")
    private BigDecimal totalContractAmount;

    @Schema(description = "已核销回款")
    private BigDecimal verifiedAmount;

    @Schema(description = "待收金额（合同金额-已核销回款）")
    private BigDecimal pendingAmount;

    @Schema(description = "本月新增客户")
    private long monthNewCustomer;

    @Schema(description = "本月新增线索")
    private long monthNewClue;
}
