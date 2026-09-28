package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 版本响应
 */
@Data
public class EditionResponse {

    @Schema(description = "版本ID")
    private String id;

    @Schema(description = "版本编码(BASIC/PRO/ENTERPRISE)")
    private String code;

    @Schema(description = "版本名称")
    private String name;

    @Schema(description = "年价")
    private BigDecimal yearPrice;

    @Schema(description = "首年促销价")
    private BigDecimal firstYearPrice;

    @Schema(description = "软上限(人数)")
    private Integer softLimit;

    @Schema(description = "有效期天数")
    private Integer validityDays;

    @Schema(description = "AI月度配额(标准次数)")
    private Integer aiMonthlyQuota;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态(1启用 0停用)")
    private Integer status;
}
