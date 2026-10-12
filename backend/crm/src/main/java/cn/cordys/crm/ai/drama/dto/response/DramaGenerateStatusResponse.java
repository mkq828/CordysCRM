package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分集生成进度响应（轮询用）。
 */
@Data
public class DramaGenerateStatusResponse {

    @Schema(description = "分集ID")
    private String episodeId;

    @Schema(description = "已完成数")
    private int done;

    @Schema(description = "失败数")
    private int failed;

    @Schema(description = "进行中数")
    private int running;

    @Schema(description = "总数")
    private int total;

    @Schema(description = "分镜进度明细")
    private List<FragmentStatus> fragments;

    /**
     * 单个分镜的进度。
     */
    @Data
    public static class FragmentStatus {

        @Schema(description = "分镜ID")
        private String fragmentId;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "画面附件ID")
        private String coverAttachmentId;

        @Schema(description = "视频附件ID")
        private String videoAttachmentId;
    }
}
