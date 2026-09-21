package cn.cordys.crm.finance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务合同明细
 */
@Data
public class FinanceContractResponse {

    @Schema(description = "合同ID")
    private String contractId;

    @Schema(description = "合同名称")
    private String contractName;

    @Schema(description = "合同编号")
    private String contractNumber;

    @Schema(description = "合同金额")
    private BigDecimal amount;

    @Schema(description = "已核销回款")
    private BigDecimal verifiedAmount;

    @Schema(description = "待回款（尾款）")
    private BigDecimal pendingAmount;

    @Schema(description = "是否已结清")
    private boolean settled;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "回款明细")
    private List<FinancePaymentRecordResponse> records;
}
