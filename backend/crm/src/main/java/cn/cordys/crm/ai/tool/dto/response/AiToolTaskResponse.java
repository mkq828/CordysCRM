package cn.cordys.crm.ai.tool.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 工具中心异步任务视图。
 */
@Data
public class AiToolTaskResponse {

    @Schema(description = "任务ID")
    private String id;

    @Schema(description = "工具类型: IMAGE/TRANSCODE")
    private String type;

    @Schema(description = "任务展示名")
    private String name;

    @Schema(description = "状态: PENDING/PROCESSING/DONE/FAILED")
    private String status;

    @Schema(description = "进度百分比(0-100)")
    private Integer progress;

    @Schema(description = "输出附件ID（DONE 后有值）")
    private String attachmentId;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;
}
