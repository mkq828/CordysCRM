package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 销售个人经营明细（经营大屏下钻：按负责人）。
 */
@Data
public class SalesOverviewResponse {

    @Schema(description = "核心指标")
    private SalesKpi kpi;

    @Schema(description = "近12月合同金额趋势")
    private List<TrendPoint> contractTrend;

    @Schema(description = "近12月回款金额趋势")
    private List<TrendPoint> paymentTrend;

    @Schema(description = "合同阶段分布")
    private List<DistributionItem> stageDist;

    @Schema(description = "客户列表")
    private List<CustomerBrief> customerList;
}
