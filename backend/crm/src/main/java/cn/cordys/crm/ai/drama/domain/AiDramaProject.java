package cn.cordys.crm.ai.drama.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 漫剧项目：一部漫剧的总载体，下挂剧本、分集、资产库。
 */
@Data
@Table(name = "ai_drama_project")
public class AiDramaProject extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "剧名")
    private String title;

    @Schema(description = "创意/一句话描述")
    private String description;

    @Schema(description = "工作流: script(剧本)")
    private String workflow;

    @Schema(description = "预留画布 JSON（本版不用）")
    private String contentJson;

    @Schema(description = "项目全局成片设置 JSON(画幅/清晰度/画面风格/字幕)")
    private String paramsJson;

    @Schema(description = "封面附件ID")
    private String coverAttachmentId;
}
