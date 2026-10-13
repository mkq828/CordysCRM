package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 经营大屏总览响应（老板视角，全租户聚合）。
 */
@Data
public class BusinessScreenOverviewResponse {

    @Schema(description = "核心指标卡片")
    private BusinessScreenKpi kpi;

    @Schema(description = "近12月合同金额趋势")
    private List<TrendPoint> contractTrend;

    @Schema(description = "近12月回款金额趋势")
    private List<TrendPoint> paymentTrend;

    @Schema(description = "销售业绩排行（按负责人）")
    private List<RankItem> salesRank;

    @Schema(description = "合同阶段分布")
    private List<DistributionItem> contractStageDist;

    @Schema(description = "客户省份分布（省级 adcode + 客户数）")
    private List<RegionItem> customerRegionDist;

    @Schema(description = "转化漏斗（线索→客户→商机→订单→合同）")
    private Funnel funnel;
}
