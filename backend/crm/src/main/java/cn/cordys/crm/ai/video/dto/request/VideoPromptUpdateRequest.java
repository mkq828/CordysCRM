package cn.cordys.crm.ai.video.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新内置提示词请求（风格/角色/额外），任一为 null 则保持不变；空串表示清空该提示词。
 */
@Data
public class VideoPromptUpdateRequest {

    @Schema(description = "风格提示词（null 不变，空串清空）")
    private String stylePrompt;

    @Schema(description = "角色提示词（null 不变，空串清空）")
    private String characterPrompt;

    @Schema(description = "额外要求（null 不变，空串清空）")
    private String extraPrompt;
}
