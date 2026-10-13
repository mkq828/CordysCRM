package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 阶段合同列表项（经营大屏下钻：按合同阶段）。
 */
@Data
public class StageContractItem {

    @Schema(description = "合同ID")
    private String contractId;

    @Schema(description = "合同编号")
    private String contractNumber;

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "合同金额")
    private BigDecimal amount;

    @Schema(description = "负责人ID")
    private String owner;

    @Schema(description = "负责人姓名")
    private String ownerName;

    @Schema(description = "签约日期，格式 yyyy-MM-dd")
    private String signDate;

    @Schema(description = "已核销回款")
    private BigDecimal verifiedAmount;
}
