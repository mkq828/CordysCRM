package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租户续费/升级申请（个人中心自助提交，admin 核销后开通/升级套餐）
 */
@Data
@Table(name = "tenant_plan_application")
public class TenantPlanApplication extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "租户名（冗余）")
    private String orgName;

    @Schema(description = "当前版本")
    private String currentVersion;

    @Schema(description = "目标版本编码(BASIC/PRO/ENTERPRISE)")
    private String targetVersion;

    @Schema(description = "成交价")
    private BigDecimal amount;

    @Schema(description = "金额计算过程")
    private String priceDetail;

    @Schema(description = "有效期天数")
    private Integer validityDays;

    @Schema(description = "付款方式：WECHAT/TRANSFER/ALIPAY/OFFLINE")
    private String paymentType;

    @Schema(description = "付款凭证附件ID（逗号分隔）")
    private String voucherIds;

    @Schema(description = "状态：PENDING/APPROVED/CANCELLED")
    private String status;

    @Schema(description = "核销后补发合同ID")
    private String contractId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "审核备注（驳回原因/通过说明）")
    private String verifyRemark;
}
