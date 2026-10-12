package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分镜口播试听响应（base64 WAV）。
 */
@Data
public class DramaVoicePreviewResponse {

    @Schema(description = "base64 编码的 WAV 音频")
    private String audio;

    @Schema(description = "音色标识")
    private String voice;
}
