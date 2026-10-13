package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 趋势点（月 + 金额）。
 */
@Data
public class TrendPoint {

    @Schema(description = "月份，格式 yyyy-MM")
    private String month;

    @Schema(description = "金额")
    private BigDecimal amount;
}
