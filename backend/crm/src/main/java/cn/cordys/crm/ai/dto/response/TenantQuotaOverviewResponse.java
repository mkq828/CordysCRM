package cn.cordys.crm.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租户端 AI 额度总览
 */
@Data
public class TenantQuotaOverviewResponse {

    @Schema(description = "统计月份(yyyy-MM)")
    private String period;

    @Schema(description = "本月已用标准次数")
    private BigDecimal usedCalls;

    @Schema(description = "本月配额(标准次数)")
    private Integer quota;

    @Schema(description = "剩余次数(配额-已用，下限0)")
    private BigDecimal remainingCalls;

    @Schema(description = "已用占比(%)")
    private BigDecimal usedPercent;

    @Schema(description = "软超阈值(%)")
    private Integer softLimitPercent;

    @Schema(description = "今日已用标准次数")
    private BigDecimal todayCalls;

    @Schema(description = "额度状态(NORMAL/SOFT_LIMITED/HARD_LIMITED/RATE_LIMITED/CIRCUIT_BROKEN)")
    private String status;
}
