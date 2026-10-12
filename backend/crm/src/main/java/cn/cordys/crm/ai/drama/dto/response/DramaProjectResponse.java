package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 漫剧项目响应（列表项与详情共用，详情多带 script）。
 */
@Data
public class DramaProjectResponse {

    @Schema(description = "项目ID")
    private String id;

    @Schema(description = "剧名")
    private String title;

    @Schema(description = "创意/一句话描述")
    private String description;

    @Schema(description = "工作流")
    private String workflow;

    @Schema(description = "封面附件ID")
    private String coverAttachmentId;

    @Schema(description = "项目全局成片设置 JSON")
    private String paramsJson;

    @Schema(description = "分集数")
    private Integer episodeCount;

    @Schema(description = "资产数")
    private Integer assetCount;

    @Schema(description = "是否有剧本")
    private Boolean hasScript;

    @Schema(description = "剧本（详情时带）")
    private DramaScriptResponse script;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;

    @Schema(description = "更新时间(毫秒)")
    private Long updateTime;
}
