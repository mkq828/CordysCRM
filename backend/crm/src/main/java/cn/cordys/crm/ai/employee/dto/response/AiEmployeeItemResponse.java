package cn.cordys.crm.ai.employee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 员工角色目录-列表项（不含系统提示词，保持列表轻量）。
 */
@Data
public class AiEmployeeItemResponse {

    @Schema(description = "角色ID")
    private String id;

    @Schema(description = "部门英文标识")
    private String division;

    @Schema(description = "角色名")
    private String name;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "品牌色(hex)")
    private String color;

    @Schema(description = "emoji 图标")
    private String emoji;

    @Schema(description = "一句话人设")
    private String vibe;
}
