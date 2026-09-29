package cn.cordys.crm.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 平台端 AI 额度-恢复默认额度请求
 */
@Data
public class AiTenantQuotaResetRequest {

    @Schema(description = "租户组织ID")
    @NotBlank(message = "organizationId 不能为空")
    private String organizationId;
}
