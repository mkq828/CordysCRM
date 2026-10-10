package cn.cordys.crm.ai.voiceclone.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 声音克隆音色（列表与详情复用）。
 */
@Data
public class CloneVoiceResponse {

    @Schema(description = "音色ID")
    private String id;

    @Schema(description = "音色名称")
    private String name;

    @Schema(description = "服务商")
    private String provider;

    @Schema(description = "火山引擎 custom_speaker_id")
    private String speakerId;

    @Schema(description = "状态: TRAINING/SUCCESS/FAILED")
    private String status;

    @Schema(description = "样本音频附件ID")
    private String sampleAttachmentId;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;
}
