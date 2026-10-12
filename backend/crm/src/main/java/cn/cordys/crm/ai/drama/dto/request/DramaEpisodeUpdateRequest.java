package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新漫剧分集请求。
 */
@Data
public class DramaEpisodeUpdateRequest {

    @Schema(description = "集名")
    private String name;

    @Schema(description = "附加信息 JSON")
    private String paramsJson;
}
