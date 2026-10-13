package cn.cordys.crm.ai.replica.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 爆款复刻任务（功能 7 扩展 · 换皮变体）：一条真实参考视频，解析后生成 N 个换包装变体。
 */
@Data
@Table(name = "ai_replica_task")
public class AiReplicaTask extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "任务名(默认参考视频文件名)")
    private String title;

    @Schema(description = "参考视频附件ID")
    private String referenceAttachmentId;

    @Schema(description = "解析状态: PENDING/PARSING/READY/FAILED")
    private String parseStatus;

    @Schema(description = "解析结果 JSON(sentences[{text,beginMs,endMs}] + cover_attachment_id + duration_ms)")
    private String parseJson;

    @Schema(description = "变体数量(1~5)")
    private Integer variantCount;

    @Schema(description = "整体状态: PENDING/PARSING/GENERATING/READY/FAILED")
    private String status;

    @Schema(description = "失败原因")
    private String errorMsg;
}
