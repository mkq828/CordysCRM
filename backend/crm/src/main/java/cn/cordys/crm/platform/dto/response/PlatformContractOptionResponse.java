package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台合同下拉选项（回款/发票选合同用）
 */
@Data
public class PlatformContractOptionResponse {

    @Schema(description = "合同ID")
    private String id;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "租户名称")
    private String orgName;

    @Schema(description = "套餐版本名称")
    private String editionName;

    @Schema(description = "合同金额(应收)")
    private BigDecimal amount;
}
