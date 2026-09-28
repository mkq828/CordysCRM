package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台发票保存请求（新增/编辑）
 */
@Data
public class PlatformInvoiceSaveRequest {

    @Schema(description = "主键(编辑时传)")
    private String id;

    @Schema(description = "平台合同ID")
    private String contractId;

    @Schema(description = "发票号")
    private String invoiceNo;

    @Schema(description = "发票类型：NORMAL普票/SPECIAL专票")
    private String invoiceType;

    @Schema(description = "开票金额(发票)")
    private BigDecimal amount;

    @Schema(description = "税率(%)，默认6")
    private BigDecimal taxRate;

    @Schema(description = "开票抬头")
    private String businessTitle;

    @Schema(description = "备注")
    private String remark;
}
