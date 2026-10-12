package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生成旁白/角色配音请求。
 */
@Data
public class DramaVoiceGenerateRequest {

    @NotBlank(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "音色资产ID（为空则新建）")
    private String assetId;

    @Schema(description = "音色名（新建时用）")
    private String name;

    @Schema(description = "配音文本")
    private String voicePrompt;

    @Schema(description = "试听样例文本")
    private String sampleText;

    @Schema(description = "音色标识")
    private String speaker;
}
