package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 区域经营明细（经营大屏下钻：按省级行政区划）。
 */
@Data
public class RegionDetailResponse {

    @Schema(description = "核心指标")
    private RegionKpi kpi;

    @Schema(description = "客户列表")
    private List<CustomerBrief> customerList;

    @Schema(description = "销售分布（按负责人）")
    private List<RankItem> salesDist;
}
