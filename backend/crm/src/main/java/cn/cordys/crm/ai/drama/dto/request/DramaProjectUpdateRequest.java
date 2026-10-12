package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新漫剧项目请求。
 */
@Data
public class DramaProjectUpdateRequest {

    @Schema(description = "剧名")
    private String title;

    @Schema(description = "创意/一句话描述")
    private String description;

    @Schema(description = "项目全局成片设置 JSON")
    private String paramsJson;
}
