package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 阶段合同明细（经营大屏下钻：按合同阶段）。
 */
@Data
public class StageDetailResponse {

    @Schema(description = "阶段名称")
    private String stageName;

    @Schema(description = "合同数")
    private long count;

    @Schema(description = "合同金额合计")
    private BigDecimal totalAmount;

    @Schema(description = "合同列表")
    private List<StageContractItem> list;
}
