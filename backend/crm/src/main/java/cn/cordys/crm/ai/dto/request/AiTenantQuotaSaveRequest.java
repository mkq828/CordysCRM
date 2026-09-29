package cn.cordys.crm.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 平台端 AI 额度-按租户调额请求
 */
@Data
public class AiTenantQuotaSaveRequest {

    @Schema(description = "租户组织ID")
    @NotBlank(message = "organizationId 不能为空")
    private String organizationId;

    @Schema(description = "覆盖后的AI月度额度(标准次数)，0=停用AI")
    @NotNull(message = "quota 不能为空")
    private Integer quota;
}
