package cn.cordys.crm.ai.video.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提交短视频生成任务请求。
 */
@Data
public class VideoGenerateRequest {

    @NotBlank(message = "主题不能为空")
    @Schema(description = "主题/标题")
    private String title;

    @Schema(description = "补充素材/卖点（可选）")
    private String extraText;

    @Schema(description = "音色标识，默认 longxiaochun（阿里云 CosyVoice 女声）")
    private String voice;

    @Schema(description = "风格，如 口播/促销，默认 口播")
    private String style;
}
