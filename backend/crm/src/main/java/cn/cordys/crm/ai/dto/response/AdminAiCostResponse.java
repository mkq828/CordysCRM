package cn.cordys.crm.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 平台端 AI 成本看板（估算，token×模型单价）
 */
@Data
public class AdminAiCostResponse {

    @Schema(description = "总成本(元，估算)")
    private BigDecimal totalCost;

    @Schema(description = "总 token 数")
    private Long totalTokens;

    @Schema(description = "总标准次数")
    private BigDecimal totalCalls;

    @Schema(description = "分租户成本行")
    private List<AdminAiCostTenantRow> tenantRows;

    @Schema(description = "成本趋势(按桶)")
    private List<AiCostTrendPoint> series;
}
