package cn.cordys.crm.ai.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 月度累计（按 org+period 记本月已用次数，天然按月重置）
 */
@Data
@Table(name = "ai_quota_usage")
public class AiQuotaUsage extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "月份(yyyy-MM)")
    private String period;

    @Schema(description = "本月已用标准次数")
    private BigDecimal usedCalls;
}
