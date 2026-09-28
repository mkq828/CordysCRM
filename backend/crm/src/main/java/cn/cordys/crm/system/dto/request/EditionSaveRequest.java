package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 版本保存请求（新增/编辑）
 */
@Data
public class EditionSaveRequest {

    @Schema(description = "版本ID，为空时新增")
    private String id;

    @Schema(description = "版本编码(BASIC/PRO/ENTERPRISE)")
    @NotBlank(message = "版本编码不能为空")
    private String code;

    @Schema(description = "版本名称")
    @NotBlank(message = "版本名称不能为空")
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

    @Schema(description = "归属功能ID集合")
    private List<String> featureIds;
}
