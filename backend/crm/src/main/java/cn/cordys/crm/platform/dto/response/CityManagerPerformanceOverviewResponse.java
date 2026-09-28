package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 城市经理业绩看板总览（按经理汇总 + 时间序列）
 */
@Data
public class CityManagerPerformanceOverviewResponse {

    @Schema(description = "按经理汇总(admin 空=全部经理逐条；city_manager=本人一条)")
    private List<CityManagerPerformanceSummary> summary;

    @Schema(description = "时间序列(跨所选经理聚合)")
    private List<CityManagerPerformancePoint> series;
}
