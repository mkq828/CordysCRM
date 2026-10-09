package cn.cordys.crm.ai.tool.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 工具中心异步任务：文生图、视频转格式等耗时工具，输入参数 JSON 化，输出落附件系统。
 */
@Data
@Table(name = "ai_tool_task")
public class AiToolTask extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "工具类型: IMAGE文生图/TRANSCODE视频转格式")
    private String type;

    @Schema(description = "任务展示名(提示词摘要/源文件名)")
    private String name;

    @Schema(description = "入参 JSON(文生图:prompt/size；转格式:sourceAttachmentId/format)")
    private String params;

    @Schema(description = "状态: PENDING/PROCESSING/DONE/FAILED")
    private String status;

    @Schema(description = "进度百分比(0-100)")
    private Integer progress;

    @Schema(description = "输出附件ID(附件系统)")
    private String attachmentId;

    @Schema(description = "失败原因")
    private String errorMsg;
}
