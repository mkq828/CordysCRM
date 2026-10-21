package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 定时任务执行日志。记录 Quartz 任务起止/耗时/成败，供平台管理员监控。
 */
@Data
@Table(name = "sys_job_exec_log")
public class JobExecLog {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "任务名(简单类名)")
    private String jobName;

    @Schema(description = "任务组")
    private String jobGroup;

    @Schema(description = "执行状态(RUNNING/SUCCESS/FAILED)")
    private String status;

    @Schema(description = "开始时间(毫秒)")
    private Long startTime;

    @Schema(description = "结束时间(毫秒)")
    private Long endTime;

    @Schema(description = "耗时(毫秒)")
    private Long durationMs;

    @Schema(description = "失败信息")
    private String errorMsg;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;
}
