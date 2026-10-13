package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户简要信息（经营大屏下钻列表项）。
 */
@Data
public class CustomerBrief {

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "客户级别（客户表无此列时为 null，前端兜底）")
    private String level;

    @Schema(description = "合同金额合计（不含作废）")
    private BigDecimal contractAmount;

    @Schema(description = "已核销回款")
    private BigDecimal verifiedAmount;
}
