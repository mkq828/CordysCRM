package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 试听分镜口播请求。
 * text 传当前编辑框里尚未保存的台词，保证「所听即所得」；不传则回退该镜已保存的台词。
 */
@Data
public class DramaVoicePreviewRequest {

    @Schema(description = "音色标识（空则用该镜已保存音色 / 模型默认音色）")
    private String voice;

    @Schema(description = "要试听的台词文本（通常为编辑框当前内容，可能尚未保存）")
    private String text;
}
