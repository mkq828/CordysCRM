package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 问题反馈列表行
 */
@Data
public class BugReportResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private String id;

    @Schema(description = "提交人id")
    private String userId;

    @Schema(description = "提交人名称")
    private String userName;

    @Schema(description = "页面路由")
    private String route;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "状态 PENDING/DONE")
    private String status;

    @Schema(description = "提交时间")
    private Long createTime;
}
