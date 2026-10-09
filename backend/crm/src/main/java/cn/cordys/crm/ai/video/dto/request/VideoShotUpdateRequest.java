package cn.cordys.crm.ai.video.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新单个分镜请求：旁白/台词与画面描述均可编辑，任一为空则保持不变。
 */
@Data
public class VideoShotUpdateRequest {

    @Schema(description = "旁白/台词（空则不改）")
    private String text;

    @Schema(description = "画面描述/逐段分镜（空则不改）")
    private String image;

    @Schema(description = "视频提示词（镜头视频就绪阶段可改，空则不改）")
    private String videoPrompt;
}
