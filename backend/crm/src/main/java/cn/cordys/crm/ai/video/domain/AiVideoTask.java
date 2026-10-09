package cn.cordys.crm.ai.video.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 短视频生成任务（功能 7 MVP）：输入主题/文案，异步产出可下载的口播视频 mp4。
 */
@Data
@Table(name = "ai_video_task")
public class AiVideoTask extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "主题/任务标题")
    private String title;

    @Schema(description = "原始输入（一句话主题或完整文案），脚本生成与分镜台大纲展示用")
    private String sourceText;

    @Schema(description = "口播文案全文(分句)")
    private String scriptText;

    @Schema(description = "配套文案(抖音/小红书/朋友圈 JSON，生成失败为 NULL)")
    private String copyJson;

    @Schema(description = "分镜 JSON 数组 [{text,image,imageAttachmentId}]，生成中逐步回填，供分镜台展示")
    private String storyboardJson;

    @Schema(description = "音色标识")
    private String voice;

    @Schema(description = "模板名称(生成时回填，供展示)")
    private String style;

    @Schema(description = "生成时使用的模板标识(ai_video_template.template_key)")
    private String templateKey;

    @Schema(description = "成片方式(video/image_text，暂都走 Ken Burns)")
    private String mode;

    @Schema(description = "所选图片模型ID(空则取推荐)")
    private String imageModelId;

    @Schema(description = "所选视频模型ID(预留，图生视频暂未接)")
    private String videoModelId;

    @Schema(description = "风格提示词(用户可覆盖，空则随模板)")
    private String stylePrompt;

    @Schema(description = "角色提示词(用户可编辑，空则 AI 按模板与主题决定)")
    private String characterPrompt;

    @Schema(description = "额外要求(卖点/补充素材，也兼容「做成视频」带入的现成脚本)")
    private String extraPrompt;

    @Schema(description = "成片类型(talk/product/scenario/comic)，决定脚本结构与画面 prompt 策略")
    private String videoType;

    @Schema(description = "状态: PENDING/SCRIPTING/SCRIPT_READY/IMAGING/TTS/ASSETS_READY/SYNTH/DONE/FAILED")
    private String status;

    @Schema(description = "进度百分比(0-100)")
    private Integer progress;

    @Schema(description = "输出视频附件ID(附件系统)")
    private String attachmentId;

    @Schema(description = "封面附件ID(可更换/用首镜，空则回退模板封面)")
    private String coverAttachmentId;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "成片比例(9:16/16:9/1:1)，出图与合成按该比例输出")
    private String ratio;
}
