package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 租户自助续费/升级申请请求
 */
@Data
public class TenantPlanApplyRequest {

    @Schema(description = "目标版本编码(BASIC/PRO/ENTERPRISE)")
    @NotBlank(message = "目标版本不能为空")
    private String targetEdition;

    @Schema(description = "付款方式：WECHAT微信/TRANSFER对公转账/ALIPAY支付宝/OFFLINE线下")
    @NotBlank(message = "付款方式不能为空")
    private String paymentType;

    @Schema(description = "付款凭证附件ID（逗号分隔）")
    private String voucherIds;

    @Schema(description = "备注")
    private String remark;
}
