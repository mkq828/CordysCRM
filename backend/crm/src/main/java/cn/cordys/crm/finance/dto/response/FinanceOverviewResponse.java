package cn.cordys.crm.finance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务应收概览统计
 */
@Data
public class FinanceOverviewResponse {

    @Schema(description = "应收总额（合同金额合计）")
    private BigDecimal totalContractAmount;

    @Schema(description = "已核销回款")
    private BigDecimal verifiedAmount;

    @Schema(description = "待回款（尾款）")
    private BigDecimal pendingAmount;

    @Schema(description = "待核销金额")
    private BigDecimal unverifiedAmount;

    @Schema(description = "客户数")
    private long customerCount;

    @Schema(description = "合同数")
    private long contractCount;

    @Schema(description = "未结清合同数")
    private long unsettledContractCount;
}
