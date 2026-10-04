package cn.cordys.crm.ai.employee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 员工角色目录-部门（用于左侧部门导航）。
 */
@Data
public class AiEmployeeDivisionResponse {

    @Schema(description = "部门英文标识(sales/marketing/engineering...)")
    private String division;

    @Schema(description = "部门内角色数量")
    private long count;
}
