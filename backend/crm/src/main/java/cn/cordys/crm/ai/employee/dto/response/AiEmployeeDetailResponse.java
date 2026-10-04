package cn.cordys.crm.ai.employee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 员工角色目录-详情（含完整系统提示词）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiEmployeeDetailResponse extends AiEmployeeItemResponse {

    @Schema(description = "角色提示词(系统提示，Markdown)")
    private String systemPrompt;
}
