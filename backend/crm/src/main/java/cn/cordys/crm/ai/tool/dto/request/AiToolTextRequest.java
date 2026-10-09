package cn.cordys.crm.ai.tool.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文案类工具请求：文案改写 / 爆款标题 / 口播脚本。
 */
@Data
public class AiToolTextRequest {

    @Schema(description = "工具类型: REWRITE文案改写/TITLE爆款标题/SCRIPT口播脚本")
    @NotBlank(message = "请指定工具类型")
    private String type;

    @Schema(description = "待处理的原文/主题")
    @NotBlank(message = "请输入内容")
    private String content;

    @Schema(description = "可选：改写风格/目标平台等补充要求")
    private String option;
}
