package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新漫剧剧本请求（创意/摘要/分集/风格）。
 */
@Data
public class DramaScriptUpdateRequest {

    @Schema(description = "整剧创意原始输入")
    private String source;

    @Schema(description = "剧情摘要 JSON")
    private String summaryJson;

    @Schema(description = "分集剧本 JSON 数组")
    private String episodeContentJson;

    @Schema(description = "画面风格ID")
    private String imageStyleId;

    @Schema(description = "剧本名")
    private String name;
}
