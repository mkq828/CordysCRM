package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 续费/升级申请响应
 */
@Data
public class TenantPlanApplicationResponse {

    @Schema(description = "申请ID")
    private String id;

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "租户名")
    private String orgName;

    @Schema(description = "当前版本")
    private String currentVersion;

    @Schema(description = "当前版本名称")
    private String currentVersionName;

    @Schema(description = "目标版本")
    private String targetVersion;

    @Schema(description = "目标版本名称")
    private String targetVersionName;

    @Schema(description = "成交价")
    private BigDecimal amount;

    @Schema(description = "金额计算过程")
    private String priceDetail;

    @Schema(description = "有效期天数")
    private Integer validityDays;

    @Schema(description = "付款方式")
    private String paymentType;

    @Schema(description = "付款凭证附件ID（逗号分隔）")
    private String voucherIds;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "补发合同ID")
    private String contractId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "审核备注（驳回原因/通过说明）")
    private String verifyRemark;

    @Schema(description = "收款账户-户名（按付款方式匹配平台收款账户，供租户核对打款）")
    private String accountName;

    @Schema(description = "收款账户-账号")
    private String accountNo;

    @Schema(description = "收款账户-开户行/平台")
    private String bankName;

    @Schema(description = "收款账户-收款码图片附件ID（仅微信/支付宝）")
    private String qrcode;

    @Schema(description = "创建时间")
    private Long createTime;
}
