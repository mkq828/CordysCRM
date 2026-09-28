package cn.cordys.crm.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台端 AI 成本看板-分租户行
 */
@Data
public class AdminAiCostTenantRow {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "租户名称")
    private String organizationName;

    @Schema(description = "标准次数")
    private BigDecimal costCalls;

    @Schema(description = "输入 token 数")
    private Long inputTokens;

    @Schema(description = "输出 token 数")
    private Long outputTokens;

    @Schema(description = "成本(元，估算)")
    private BigDecimal cost;
}
