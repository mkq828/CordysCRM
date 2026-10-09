package cn.cordys.crm.ai.video.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 配音音色试听请求（风格页「试听」按钮，合成约 5 秒样例音频）。
 */
@Data
public class VideoVoicePreviewRequest {

    @NotBlank(message = "音色不能为空")
    @Schema(description = "音色 ID（音色列表里的 value）")
    private String voice;
}
