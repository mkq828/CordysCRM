package cn.cordys.crm.platform.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台回款：平台收到的租户回款（平台账·现金流），核销后自动开通企业版
 */
@Data
@Table(name = "platform_payment_record")
public class PlatformPaymentRecord extends BaseModel {

    @Schema(description = "平台合同ID")
    private String contractId;

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "回款单号")
    private String recordNo;

    @Schema(description = "实收金额(现金流)")
    private BigDecimal amount;

    @Schema(description = "收款方式：TRANSFER对公转账/ALIPAY支付宝/WECHAT微信/OFFLINE线下")
    private String paymentType;

    @Schema(description = "收款账户(平台对公账户快照)")
    private String bankAccount;

    @Schema(description = "付款凭证附件ID(逗号分隔)")
    private String voucherAttachmentIds;

    @Schema(description = "核销状态：PENDING待核销/DONE已完成")
    private String verificationStatus;

    @Schema(description = "核销人")
    private String verifyUser;

    @Schema(description = "核销时间")
    private Long verifyTime;

    @Schema(description = "核销备注")
    private String verifyRemark;

    @Schema(description = "收款证明附件ID(逗号分隔)")
    private String verifyProof;

    @Schema(description = "撤回人")
    private String revokeUser;

    @Schema(description = "撤回时间")
    private Long revokeTime;

    @Schema(description = "撤回备注")
    private String revokeRemark;

    @Schema(description = "备注")
    private String remark;
}
