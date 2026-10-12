package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生成剧情摘要请求。
 */
@Data
public class DramaScriptSummaryRequest {

    @NotBlank(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "整剧创意（为空回退剧本 source_text）")
    private String creative;

    @Schema(description = "目标集数（为空由 AI 决定）")
    private Integer episodeCount;

    @Schema(description = "画面风格ID")
    private String imageStyleId;
}
