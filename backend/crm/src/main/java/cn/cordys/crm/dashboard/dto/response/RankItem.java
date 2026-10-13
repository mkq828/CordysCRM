package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售业绩排行项（按负责人）。
 */
@Data
public class RankItem {

    @Schema(description = "负责人ID")
    private String owner;

    @Schema(description = "负责人姓名")
    private String ownerName;

    @Schema(description = "合同金额（不含作废）")
    private BigDecimal contractAmount;

    @Schema(description = "合同数（不含作废）")
    private long contractCount;

    @Schema(description = "已核销回款")
    private BigDecimal verifiedAmount;

    @Schema(description = "客户数")
    private long customerCount;
}
