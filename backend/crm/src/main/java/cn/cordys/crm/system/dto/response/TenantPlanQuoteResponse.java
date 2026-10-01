package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 续费/升级报价响应（租户自助选择版本时实时计算）
 */
@Data
public class TenantPlanQuoteResponse {

    @Schema(description = "报价类型：RENEW续费/UPGRADE升级补差/DOWNGRADE降级不补差")
    private String type;

    @Schema(description = "当前版本编码")
    private String currentVersion;

    @Schema(description = "当前版本名称")
    private String currentVersionName;

    @Schema(description = "目标版本编码")
    private String targetVersion;

    @Schema(description = "目标版本名称")
    private String targetVersionName;

    @Schema(description = "剩余天数")
    private Long remainDays;

    @Schema(description = "有效期天数")
    private Integer validityDays;

    @Schema(description = "成交价")
    private BigDecimal amount;

    @Schema(description = "金额计算过程")
    private String priceDetail;
}
