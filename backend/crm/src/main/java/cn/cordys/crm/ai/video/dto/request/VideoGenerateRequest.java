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
    @Schema(description = "项目名称（短标题）")
    private String title;

    @Schema(description = "原始输入（一句话主题或完整文案），脚本生成与分镜台大纲展示用；为空回退 title")
    private String sourceText;

    @Schema(description = "额外要求/卖点/补充素材（可选，也兼容「做成视频」带入的现成脚本）")
    private String extraText;

    @Schema(description = "风格提示词（可选覆盖，空则随模板）")
    private String stylePrompt;

    @Schema(description = "角色提示词（可选，空则 AI 按模板与主题决定）")
    private String characterPrompt;

    @Schema(description = "成片方式：video=AI 视频（图生视频）/ image_text=静图成片，暂都走 Ken Burns")
    private String mode;

    @Schema(description = "成片类型：talk=口播科普(默认)/product=产品讲解/scenario=情景剧/comic=漫剧(占位延后)")
    private String videoType;

    @Schema(description = "所选图片模型ID（空则取推荐）")
    private String imageModelId;

    @Schema(description = "所选视频模型ID（预留，图生视频暂未接）")
    private String videoModelId;

    @Schema(description = "音色标识，默认 longxiaochun（阿里云 CosyVoice 女声）")
    private String voice;

    @Schema(description = "视频模板标识(ai_video_template.template_key)，为空回退默认模板")
    private String templateKey;

    @Schema(description = "成片比例：9:16 / 16:9 / 1:1，默认 9:16")
    private String ratio;
}
