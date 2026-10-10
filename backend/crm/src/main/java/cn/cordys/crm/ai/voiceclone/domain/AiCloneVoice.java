package cn.cordys.crm.ai.voiceclone.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 声音克隆音色（功能 11）：上传样本音频异步复刻，成功后可作为短视频配音音色（后续数字人/外呼复用）。
 */
@Data
@Table(name = "ai_clone_voice")
public class AiCloneVoice extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "音色名称（用户自定义展示名）")
    private String name;

    @Schema(description = "服务商（固定豆包语音）")
    private String provider;

    @Schema(description = "火山引擎 custom_speaker_id（复刻成功后可直接作为 TTS speaker 合成）")
    private String speakerId;

    @Schema(description = "状态: TRAINING/SUCCESS/FAILED")
    private String status;

    @Schema(description = "样本音频附件ID（临时上传）")
    private String sampleAttachmentId;

    @Schema(description = "失败原因")
    private String errorMsg;
}
