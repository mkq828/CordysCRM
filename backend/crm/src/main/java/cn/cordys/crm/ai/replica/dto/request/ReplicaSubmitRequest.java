package cn.cordys.crm.ai.replica.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 爆款复刻任务提交请求。
 */
@Data
public class ReplicaSubmitRequest {

    @NotBlank(message = "请先上传参考视频")
    @Schema(description = "参考视频附件ID（上传临时附件返回的 ID）")
    private String referenceAttachmentId;

    @Schema(description = "任务名（默认取参考视频文件名）")
    private String title;

    @Schema(description = "变体数量（默认 3，范围 1~5）")
    private Integer variantCount;
}
