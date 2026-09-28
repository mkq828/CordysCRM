package cn.cordys.crm.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 记账结果（模型调用层上报后返回）
 */
@Data
public class AiQuotaRecordResult {

    @Schema(description = "额度状态(NORMAL/SOFT_LIMITED/HARD_LIMITED/RATE_LIMITED/CIRCUIT_BROKEN)")
    private String status;

    @Schema(description = "本次折算标准次数")
    private BigDecimal costCalls;

    @Schema(description = "本月累计已用标准次数")
    private BigDecimal usedCalls;

    @Schema(description = "本月配额(标准次数)")
    private Integer quota;
}
