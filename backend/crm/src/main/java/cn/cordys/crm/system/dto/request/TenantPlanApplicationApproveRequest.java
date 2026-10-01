package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 续费/升级申请核销（或驳回）请求
 */
@Data
public class TenantPlanApplicationApproveRequest {

    @Schema(description = "申请ID")
    @NotBlank(message = "申请ID不能为空")
    private String id;

    @Schema(description = "核销后补发的合同ID（可选，核销时回填）")
    private String contractId;

    @Schema(description = "审核备注（核销选填；驳回必填，写明原因）")
    private String remark;
}
