package cn.cordys.crm.ai.voiceclone.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 克隆音色试听请求：text 留空则用默认样例文案。
 */
@Data
public class CloneVoicePreviewRequest {

    @Schema(description = "试听文案（留空用默认样例）")
    private String text;
}
