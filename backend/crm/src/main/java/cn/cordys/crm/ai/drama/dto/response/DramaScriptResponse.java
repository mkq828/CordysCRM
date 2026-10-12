package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 漫剧剧本响应。
 */
@Data
public class DramaScriptResponse {

    @Schema(description = "剧本ID")
    private String id;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "剧本名")
    private String name;

    @Schema(description = "整剧创意原始输入")
    private String sourceText;

    @Schema(description = "剧情摘要 JSON")
    private String summaryJson;

    @Schema(description = "分集剧本 JSON 数组")
    private String episodeContentJson;

    @Schema(description = "摘要生成状态: running/completed/failed")
    private String summaryStatus;

    @Schema(description = "image_style_id 等附加信息")
    private String paramsJson;
}
