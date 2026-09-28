package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台回款保存请求（新增/编辑）
 */
@Data
public class PlatformPaymentRecordSaveRequest {

    @Schema(description = "主键(编辑时传)")
    private String id;

    @Schema(description = "平台合同ID")
    private String contractId;

    @Schema(description = "回款单号")
    private String recordNo;

    @Schema(description = "实收金额(现金流)")
    private BigDecimal amount;

    @Schema(description = "收款方式：TRANSFER对公转账/ALIPAY支付宝/WECHAT微信/OFFLINE线下")
    private String paymentType;

    @Schema(description = "收款账户")
    private String bankAccount;

    @Schema(description = "付款凭证附件ID(逗号分隔)")
    private String voucherAttachmentIds;

    @Schema(description = "备注")
    private String remark;
}
