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

    @Schema(description = "口播文案全文(分句)")
    private String scriptText;

    @Schema(description = "音色标识")
    private String voice;

    @Schema(description = "风格/模板标识")
    private String style;

    @Schema(description = "状态: PENDING/SCRIPTING/TTS/SYNTH/DONE/FAILED")
    private String status;

    @Schema(description = "进度百分比(0-100)")
    private Integer progress;

    @Schema(description = "输出视频附件ID(附件系统)")
    private String attachmentId;

    @Schema(description = "失败原因")
    private String errorMsg;
}
