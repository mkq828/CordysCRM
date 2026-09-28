package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 城市经理业绩看板查询请求
 */
@Data
public class CityManagerPerformanceRequest {

    @Schema(description = "城市经理ID(空=全部，仅 admin；city_manager 强制本人)")
    private String managerId;

    @Schema(description = "聚合维度：WEEK/MONTH/YEAR，默认 MONTH")
    private String groupBy;

    @Schema(description = "开始时间(毫秒时间戳)")
    private Long startTime;

    @Schema(description = "结束时间(毫秒时间戳)")
    private Long endTime;
}
