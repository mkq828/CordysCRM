package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 追加一集响应。
 */
@Data
public class DramaAddEpisodeResponse {

    @Schema(description = "新集号")
    private int episodeNumber;

    @Schema(description = "最新剧本")
    private DramaScriptResponse script;
}
