package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台营收看板查询请求
 */
@Data
public class PlatformRevenueRequest {

    @Schema(description = "开始时间(毫秒，可空)")
    private Long startTime;

    @Schema(description = "结束时间(毫秒，可空)")
    private Long endTime;

    @Schema(description = "分组粒度：WEEK/MONTH/YEAR")
    private String groupBy;
}
