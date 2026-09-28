package cn.cordys.crm.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 额度全局配置更新（字段为空表示不修改）
 */
@Data
public class AiQuotaConfigRequest {

    @Schema(description = "1 次标准调用折算 token 数")
    private Long tokensPerCall;

    @Schema(description = "软超阈值(%)")
    private Integer softLimitPercent;

    @Schema(description = "试用租户月配额(次数)")
    private Integer trialQuota;

    @Schema(description = "单租户每分钟调用上限")
    private Integer minuteCallLimit;

    @Schema(description = "单租户每日调用上限")
    private Integer dailyCallLimit;

    @Schema(description = "单租户单日成本熔断阈值(元)")
    private Double dailyCostThreshold;
}
