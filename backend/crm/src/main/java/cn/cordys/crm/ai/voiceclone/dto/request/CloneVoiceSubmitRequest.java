package cn.cordys.crm.ai.voiceclone.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 声音克隆提交请求：音色名 + 样本音频附件ID（临时上传）。
 */
@Data
public class CloneVoiceSubmitRequest {

    @Schema(description = "音色名称")
    @NotBlank(message = "请填写音色名称")
    private String name;

    @Schema(description = "样本音频附件ID（/attachment/upload/temp 上传返回）")
    @NotBlank(message = "请上传样本音频")
    private String sampleAttachmentId;
}
