package cn.cordys.crm.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租户端 AI 用量趋势点（按桶聚合）
 */
@Data
public class AiQuotaTrendPoint {

    @Schema(description = "时间桶(按 groupBy：yyyy-MM-dd / yyyy-MM)")
    private String bucket;

    @Schema(description = "该桶已用标准次数")
    private BigDecimal usedCalls;
}
