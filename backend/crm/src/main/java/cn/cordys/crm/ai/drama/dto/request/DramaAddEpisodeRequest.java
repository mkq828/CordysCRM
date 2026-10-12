package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 追加一集请求。
 */
@Data
public class DramaAddEpisodeRequest {

    @NotBlank(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "集名")
    private String title;
}
