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

    @Schema(description = "音色标识")
    private String voice;

    @Schema(description = "风格")
    private String style;

    @Schema(description = "状态: PENDING/SCRIPTING/TTS/SYNTH/DONE/FAILED")
    private String status;

    @Schema(description = "进度百分比(0-100)")
    private Integer progress;

    @Schema(description = "输出视频附件ID，DONE 后可下载")
    private String attachmentId;

    @Schema(description = "配套文案(抖音/小红书/朋友圈 JSON，供前端渲染复制)")
    private String copyJson;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;
}
