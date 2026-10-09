package cn.cordys.crm.ai.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 已购额度包剩余：admin 确认订单时 +calls，生成调用超出月配额时按 feature 扣减；featureCode 空串 = 通用包。
 */
@Data
@Table(name = "ai_quota_pack_balance")
public class AiQuotaPackBalance extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "AI 功能编码，空串=通用包")
    private String featureCode;

    @Schema(description = "剩余次数")
    private BigDecimal remainingCalls;
}
