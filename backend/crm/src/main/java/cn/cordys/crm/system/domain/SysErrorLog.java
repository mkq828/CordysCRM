package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 系统错误日志。全局 ERROR 采集落库，供平台管理员在生产报错后按 traceId 本地复现排查。
 */
@Data
@Table(name = "sys_error_log")
public class SysErrorLog {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "报错类(Logger名)")
    private String loggerName;

    @Schema(description = "错误信息")
    private String message;

    @Schema(description = "异常堆栈")
    private String stackTrace;

    @Schema(description = "发生时间(毫秒)")
    private Long createTime;
}
