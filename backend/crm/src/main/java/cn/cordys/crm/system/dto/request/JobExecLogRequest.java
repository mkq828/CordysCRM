package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 定时任务执行日志分页查询请求。平台级监控，仅 admin 查询。
 */
@Data
public class JobExecLogRequest extends BasePageRequest {

    @Schema(description = "任务名(模糊匹配)")
    private String jobName;

    @Schema(description = "执行状态(RUNNING/SUCCESS/FAILED)")
    private String status;

    @Schema(description = "开始时间(毫秒)")
    private Long startTime;

    @Schema(description = "结束时间(毫秒)")
    private Long endTime;
}
