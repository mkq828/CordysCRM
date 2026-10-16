package cn.cordys.crm.ai.attribution.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 经营归因结论。
 */
@Data
@Schema(description = "经营归因响应")
public class AttributionResponse {

    @Schema(description = "归因结论(自然语言)")
    private String content;
}
