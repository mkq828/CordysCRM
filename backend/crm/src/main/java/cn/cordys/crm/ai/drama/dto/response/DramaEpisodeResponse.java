package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 漫剧分集响应（含分镜列表）。
 */
@Data
public class DramaEpisodeResponse {

    @Schema(description = "分集ID")
    private String id;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "集名")
    private String name;

    @Schema(description = "集序")
    private Integer sortOrder;

    @Schema(description = "分镜规划状态")
    private String status;

    @Schema(description = "合成成片附件ID(mp4)")
    private String videoAttachmentId;

    @Schema(description = "配音状态: running/completed/failed")
    private String narrationStatus;

    @Schema(description = "逐镜视频状态: running/completed/failed")
    private String videoStatus;

    @Schema(description = "逐镜视频失败原因")
    private String videoError;

    @Schema(description = "合成状态: running/completed/failed")
    private String composeStatus;

    @Schema(description = "合成失败原因")
    private String composeError;

    @Schema(description = "分镜列表")
    private List<DramaFragmentResponse> fragments;
}
