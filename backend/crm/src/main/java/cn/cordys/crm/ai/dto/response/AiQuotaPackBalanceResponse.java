package cn.cordys.crm.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租户已购额度包余额（按功能）
 */
@Data
public class AiQuotaPackBalanceResponse {

    @Schema(description = "AI 功能编码，空串=全功能通用包")
    private String featureCode;

    @Schema(description = "功能名称")
    private String featureName;

    @Schema(description = "剩余次数")
    private BigDecimal remainingCalls;
}
