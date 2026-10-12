package cn.cordys.crm.ai.drama.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 漫剧剧本：与项目一对一，存整剧创意、剧情摘要与分集剧本。
 */
@Data
@Table(name = "ai_drama_script")
public class AiDramaScript extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "所属项目ID")
    private String projectId;

    @Schema(description = "剧本名")
    private String name;

    @Schema(description = "整剧创意原始输入")
    private String sourceText;

    @Schema(description = "剧情摘要 JSON(剧名/类型/一句话/梗概/人物)")
    private String summaryJson;

    @Schema(description = "分集剧本 JSON 数组")
    private String episodeContentJson;

    @Schema(description = "摘要生成状态: running/completed/failed")
    private String summaryStatus;

    @Schema(description = "image_style_id 等附加信息")
    private String paramsJson;
}
