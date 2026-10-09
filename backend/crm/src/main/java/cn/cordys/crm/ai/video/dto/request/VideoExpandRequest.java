package cn.cordys.crm.ai.video.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 主题/文案 AI 扩写请求（创作摘要里的「AI 生成主题」「AI 扩写文案」按钮）。
 */
@Data
public class VideoExpandRequest {

    @NotBlank(message = "内容不能为空")
    @Schema(description = "用户输入的主题或文案")
    private String topic;

    @Schema(description = "扩写模式：theme=生成更具体的主题，script=扩写为完整口播文案，style=生成画面风格提示词，prompts=一次生成风格/角色/额外三条提示词")
    private String mode;
}
