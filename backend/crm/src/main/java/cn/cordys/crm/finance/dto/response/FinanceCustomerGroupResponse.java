package cn.cordys.crm.finance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务应收客户分组
 */
@Data
public class FinanceCustomerGroupResponse {

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "客户合同总金额")
    private BigDecimal totalAmount;

    @Schema(description = "已核销回款")
    private BigDecimal verifiedAmount;

    @Schema(description = "待回款（尾款）")
    private BigDecimal pendingAmount;

    @Schema(description = "是否已结清")
    private boolean settled;

    @Schema(description = "客户创建时间")
    private Long customerCreateTime;

    @Schema(description = "合同列表")
    private List<FinanceContractResponse> contracts;
}
