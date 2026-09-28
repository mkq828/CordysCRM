package cn.cordys.crm.finance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务回款明细
 */
@Data
public class FinancePaymentRecordResponse {

    @Schema(description = "回款记录ID")
    private String id;

    @Schema(description = "收款账户名称")
    private String bankAccountName;

    @Schema(description = "收款方式(BANK_CARD/微信/支付宝)")
    private String bankAccountType;

    @Schema(description = "银行账号")
    private String bankAccountNo;

    @Schema(description = "开户行")
    private String bankAccountOpeningBank;

    @Schema(description = "付款凭证附件")
    private List<FinanceAttachmentResponse> vouchers;

    @Schema(description = "合同ID")
    private String contractId;

    @Schema(description = "回款编号")
    private String no;

    @Schema(description = "回款名称")
    private String name;

    @Schema(description = "回款金额")
    private BigDecimal recordAmount;

    @Schema(description = "回款时间")
    private Long recordEndTime;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "核销状态 PENDING/DONE")
    private String verificationStatus;

    @Schema(description = "核销人")
    private String verifyUser;

    @Schema(description = "核销人名称")
    private String verifyUserName;

    @Schema(description = "核销时间")
    private Long verifyTime;

    @Schema(description = "核销备注")
    private String verifyRemark;

    @Schema(description = "收款证明附件ID(逗号分隔)")
    private String verifyProof;

    @Schema(description = "撤回人")
    private String revokeUser;

    @Schema(description = "撤回人名称")
    private String revokeUserName;

    @Schema(description = "撤回时间")
    private Long revokeTime;

    @Schema(description = "撤回备注")
    private String revokeRemark;
}
