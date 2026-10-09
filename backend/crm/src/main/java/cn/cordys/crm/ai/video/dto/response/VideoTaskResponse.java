package cn.cordys.crm.ai.video.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 短视频生成任务（列表/详情）响应。
 */
@Data
public class VideoTaskResponse {

    @Schema(description = "任务ID")
    private String id;

    @Schema(description = "主题/任务标题")
    private String title;

    @Schema(description = "原始输入（一句话主题或完整文案），分镜台大纲展示用")
    private String sourceText;

    @Schema(description = "音色标识")
    private String voice;

    @Schema(description = "模板名称(生成时回填)")
    private String style;

    @Schema(description = "生成时使用的模板标识")
    private String templateKey;

    @Schema(description = "成片方式(video/image_text)")
    private String mode;

    @Schema(description = "成片类型(talk/product/scenario/comic)")
    private String videoType;

    @Schema(description = "所选图片模型ID")
    private String imageModelId;

    @Schema(description = "所选视频模型ID(预留)")
    private String videoModelId;

    @Schema(description = "风格提示词(可回填编辑弹窗)")
    private String stylePrompt;

    @Schema(description = "角色提示词(可回填编辑弹窗)")
    private String characterPrompt;

    @Schema(description = "额外要求(可回填编辑弹窗)")
    private String extraPrompt;

    @Schema(description = "状态: PENDING/SCRIPTING/SCRIPT_READY/IMAGING/TTS/ASSETS_READY/SYNTH/DONE/FAILED")
    private String status;

    @Schema(description = "进度百分比(0-100)")
    private Integer progress;

    @Schema(description = "输出视频附件ID，DONE 后可下载")
    private String attachmentId;

    @Schema(description = "封面附件ID(可更换/用首镜，空则回退模板封面)")
    private String coverAttachmentId;

    @Schema(description = "配套文案(抖音/小红书/朋友圈 JSON，供前端渲染复制)")
    private String copyJson;

    @Schema(description = "分镜 JSON 数组 [{text,image,imageAttachmentId}]，供分镜台展示")
    private String storyboardJson;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;
}
