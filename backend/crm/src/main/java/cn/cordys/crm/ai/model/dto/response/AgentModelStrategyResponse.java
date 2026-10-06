package cn.cordys.crm.ai.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 模型路由策略响应。
 */
@Data
public class AgentModelStrategyResponse {

    @Schema(description = "对话/通用模型ID列表")
    private List<String> chatModels;

    @Schema(description = "任务模型ID列表")
    private List<String> taskModels;

    @Schema(description = "配音模型ID列表（功能7 TTS）")
    private List<String> ttsModels;

    @Schema(description = "是否自动降级")
    private Boolean fallback;
}
