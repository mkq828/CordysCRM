package cn.cordys.crm.ai.tool.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文生图工具请求。
 */
@Data
public class AiToolImageRequest {

    @Schema(description = "画面描述提示词")
    @NotBlank(message = "请输入画面描述")
    private String prompt;

    @Schema(description = "输出尺寸，如 1080x1920 竖屏，默认 1080x1920")
    private String size;
}
