package cn.cordys.crm.ai.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 用量明细（每次模型调用一条，input/output token 分开，折算标准次数）
 */
@Data
@Table(name = "ai_usage_record")
public class AiUsageRecord extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "AI功能编码(sys_feature.feature_code)")
    private String featureCode;

    @Schema(description = "模型编码(ai_model_price.model_code)")
    private String modelCode;

    @Schema(description = "输入token数")
    private Long inputTokens;

    @Schema(description = "输出token数")
    private Long outputTokens;

    @Schema(description = "总token数")
    private Long totalTokens;

    @Schema(description = "折算标准次数")
    private BigDecimal costCalls;

    @Schema(description = "当次额度状态(NORMAL/SOFT_LIMITED/HARD_LIMITED)")
    private String status;
}
