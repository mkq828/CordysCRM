package cn.cordys.crm.ai.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 模型下拉选项（对齐前端 AiModelOption）。
 */
@Data
public class AgentModelOptionResponse {

    @Schema(description = "模型ID")
    private String id;

    @Schema(description = "模型展示名称")
    private String name;

    @Schema(description = "模型ID（字符串）")
    private String idAsString;
}
