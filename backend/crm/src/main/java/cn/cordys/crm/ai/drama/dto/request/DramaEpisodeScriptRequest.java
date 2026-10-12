package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生成分集剧本请求。
 */
@Data
public class DramaEpisodeScriptRequest {

    @NotBlank(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "单批生成集数（默认 1）")
    private Integer batchSize;

    @Schema(description = "强制重新生成全部")
    private Boolean force;

    @Schema(description = "指定生成某一集（1 起）")
    private Integer episodeNumber;
}
