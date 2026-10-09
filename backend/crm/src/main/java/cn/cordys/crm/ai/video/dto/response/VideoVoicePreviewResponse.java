package cn.cordys.crm.ai.video.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 配音音色试听响应：audio 为合成音频的 base64（WAV），前端解码为 Blob 播放。
 */
@Data
public class VideoVoicePreviewResponse {

    @Schema(description = "合成音频 base64（WAV）")
    private String audio;
}
