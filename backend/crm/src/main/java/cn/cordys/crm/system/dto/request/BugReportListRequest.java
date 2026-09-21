package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 问题反馈列表请求（admin）
 */
@Data
public class BugReportListRequest extends BasePageRequest {

    @Schema(description = "状态 PENDING/DONE")
    private String status;

    @Schema(description = "关键字（描述/提交人）")
    private String keyword;

    @Schema(description = "开始时间")
    private Long startTime;

    @Schema(description = "结束时间")
    private Long endTime;
}
