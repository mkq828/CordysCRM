package cn.cordys.crm.ai.replica.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 爆款复刻任务响应（含变体列表，供前端轮询）。
 */
@Data
public class ReplicaTaskResponse {

    @Schema(description = "任务ID")
    private String id;

    @Schema(description = "任务名")
    private String title;

    @Schema(description = "参考视频附件ID")
    private String referenceAttachmentId;

    @Schema(description = "封面帧附件ID")
    private String coverAttachmentId;

    @Schema(description = "视频时长(毫秒)")
    private Long durationMs;

    @Schema(description = "解析状态: PENDING/PARSING/READY/FAILED")
    private String parseStatus;

    @Schema(description = "整体状态: PENDING/PARSING/GENERATING/READY/FAILED")
    private String status;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "变体列表")
    private List<ReplicaVariantResponse> variants;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;
}
