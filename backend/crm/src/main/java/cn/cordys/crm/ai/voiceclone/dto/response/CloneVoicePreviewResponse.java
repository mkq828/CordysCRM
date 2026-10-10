package cn.cordys.crm.ai.voiceclone.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 克隆音色试听结果（audio 为 base64 WAV）。
 */
@Data
public class CloneVoicePreviewResponse {

    @Schema(description = "试听音频（base64 WAV）")
    private String audio;
}
