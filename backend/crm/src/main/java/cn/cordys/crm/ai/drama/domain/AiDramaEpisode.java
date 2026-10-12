package cn.cordys.crm.ai.drama.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 漫剧分集：项目下的一集，下挂多个分镜片段。
 */
@Data
@Table(name = "ai_drama_episode")
public class AiDramaEpisode extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "所属项目ID")
    private String projectId;

    @Schema(description = "集名")
    private String name;

    @Schema(description = "集序")
    private Integer sortOrder;

    @Schema(description = "分镜规划状态: planning/completed/failed")
    private String status;

    @Schema(description = "合成成片附件ID(mp4)")
    private String videoAttachmentId;

    @Schema(description = "分镜规划/生成/合成附加信息 JSON")
    private String paramsJson;
}
