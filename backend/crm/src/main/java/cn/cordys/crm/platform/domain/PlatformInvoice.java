package cn.cordys.crm.platform.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台发票：平台开给租户的发票（平台账·开票），三个金额口径之一
 */
@Data
@Table(name = "platform_invoice")
public class PlatformInvoice extends BaseModel {

    @Schema(description = "平台合同ID")
    private String contractId;

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "发票号")
    private String invoiceNo;

    @Schema(description = "发票类型：NORMAL普票/SPECIAL专票")
    private String invoiceType;

    @Schema(description = "开票金额(发票)")
    private BigDecimal amount;

    @Schema(description = "税率(%)，默认6")
    private BigDecimal taxRate;

    @Schema(description = "税额")
    private BigDecimal taxAmount;

    @Schema(description = "开票状态：NOT_INVOICED未开/INVOICED已开/VOIDED作废")
    private String invoiceStatus;

    @Schema(description = "开票抬头")
    private String businessTitle;

    @Schema(description = "备注")
    private String remark;
}
