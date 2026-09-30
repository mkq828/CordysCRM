package cn.cordys.crm.ai.model.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 模型路由策略（agent_model_strategy 表，全局单行）。
 */
@Data
@Table(name = "agent_model_strategy")
public class AgentModelStrategy {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "对话/通用模型ID（逗号分隔）")
    private String chatModels;

    @Schema(description = "任务模型ID（逗号分隔）")
    private String taskModels;

    @Schema(description = "是否自动降级")
    private Boolean fallback;
}
