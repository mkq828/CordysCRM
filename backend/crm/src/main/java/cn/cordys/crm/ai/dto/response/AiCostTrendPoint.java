package cn.cordys.crm.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台端 AI 成本趋势点（按桶聚合）
 */
@Data
public class AiCostTrendPoint {

    @Schema(description = "时间桶(按 groupBy：yyyy-MM-dd / yyyy-MM)")
    private String bucket;

    @Schema(description = "该桶成本(元，估算)")
    private BigDecimal cost;
}
