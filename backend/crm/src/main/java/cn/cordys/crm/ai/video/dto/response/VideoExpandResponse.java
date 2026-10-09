package cn.cordys.crm.ai.video.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 主题/文案 AI 扩写响应：title 用于自动填充项目名称，content 用于填充输入框。
 */
@Data
public class VideoExpandResponse {

    @Schema(description = "扩写出的标题（供项目名称自动填充）")
    private String title;

    @Schema(description = "扩写后的内容（主题或完整文案）")
    private String content;

    @Schema(description = "生成的风格提示词（mode=prompts 时返回）")
    private String stylePrompt;

    @Schema(description = "生成的角色提示词（mode=prompts 时返回）")
    private String characterPrompt;

    @Schema(description = "生成的额外提示词（mode=prompts 时返回）")
    private String extraPrompt;
}
