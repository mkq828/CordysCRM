package cn.cordys.crm.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 验证用记账请求（模型调用联网后由模型层替代，上线前删除）
 */
@Data
public class AiMockRecordRequest {

    @Schema(description = "租户组织ID")
    @NotBlank(message = "organizationId 不能为空")
    private String organizationId;

    @Schema(description = "AI功能编码")
    @NotBlank(message = "featureCode 不能为空")
    private String featureCode;

    @Schema(description = "模型编码")
    @NotBlank(message = "modelCode 不能为空")
    private String modelCode;

    @Schema(description = "输入token数")
    @NotNull(message = "inputTokens 不能为空")
    private Long inputTokens;

    @Schema(description = "输出token数")
    @NotNull(message = "outputTokens 不能为空")
    private Long outputTokens;
}
