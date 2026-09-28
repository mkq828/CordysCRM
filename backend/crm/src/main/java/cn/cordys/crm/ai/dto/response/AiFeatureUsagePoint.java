package cn.cordys.crm.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租户端 AI 分功能用量
 */
@Data
public class AiFeatureUsagePoint {

    @Schema(description = "功能编码")
    private String featureCode;

    @Schema(description = "功能名称(取自 sys_feature)")
    private String featureName;

    @Schema(description = "已用标准次数")
    private BigDecimal usedCalls;
}
