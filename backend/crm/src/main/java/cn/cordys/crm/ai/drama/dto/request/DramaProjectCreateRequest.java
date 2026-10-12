package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新建漫剧项目请求（脚本工作流）。
 */
@Data
public class DramaProjectCreateRequest {

    @NotBlank(message = "剧名不能为空")
    @Schema(description = "剧名")
    private String title;

    @Schema(description = "整剧创意/一句话描述（AI 生剧本的输入，为空回退 title）")
    private String source;

    @Schema(description = "目标集数（为空由 AI 决定）")
    private Integer episodeCount;

    @Schema(description = "画面风格ID（为空走默认）")
    private String imageStyleId;

    @Schema(description = "工作流: script（默认）")
    private String workflow;
}
