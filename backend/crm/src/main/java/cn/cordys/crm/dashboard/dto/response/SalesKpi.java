package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售个人经营指标（经营大屏下钻：按负责人）。
 */
@Data
public class SalesKpi {

    @Schema(description = "合同金额合计（不含作废）")
    private BigDecimal contractAmount;

    @Schema(description = "已核销回款")
    private BigDecimal verifiedAmount;

    @Schema(description = "待收金额（合同金额-已核销回款）")
    private BigDecimal pendingAmount;

    @Schema(description = "合同数（不含作废）")
    private long contractCount;

    @Schema(description = "客户数")
    private long customerCount;

    @Schema(description = "线索数（未转化）")
    private long clueCount;

    @Schema(description = "商机数")
    private long opportunityCount;

    @Schema(description = "本月新增客户")
    private long monthNewCustomer;

    @Schema(description = "本月新签合同数")
    private long monthNewContract;

    @Schema(description = "本月新签合同金额")
    private BigDecimal monthNewContractAmount;
}
