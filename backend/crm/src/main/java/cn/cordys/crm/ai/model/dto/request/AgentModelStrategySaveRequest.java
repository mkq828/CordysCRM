package cn.cordys.crm.ai.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 模型路由策略保存请求。
 */
@Data
public class AgentModelStrategySaveRequest {

    @Schema(description = "对话/通用模型ID列表")
    private List<String> chatModels;

    @Schema(description = "任务模型ID列表")
    private List<String> taskModels;

    @Schema(description = "是否自动降级")
    private Boolean fallback;
}
