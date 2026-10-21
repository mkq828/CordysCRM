package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 系统错误日志分页查询请求。平台级监控，仅 admin 查询。
 */
@Data
public class SysErrorLogRequest extends BasePageRequest {

    @Schema(description = "报错类(模糊匹配)")
    private String loggerName;

    @Schema(description = "链路追踪ID(精确匹配)")
    private String traceId;

    @Schema(description = "错误信息关键词(模糊匹配)")
    private String keyword;

    @Schema(description = "开始时间(毫秒)")
    private Long startTime;

    @Schema(description = "结束时间(毫秒)")
    private Long endTime;
}
