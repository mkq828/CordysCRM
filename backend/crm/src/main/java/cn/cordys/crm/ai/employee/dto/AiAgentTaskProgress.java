package cn.cordys.crm.ai.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * AI 员工「自动跑任务」的工具执行进度事件（SSE {@code progress} 帧），字段对齐前端
 * {@code AgentChatProgressData}，用于在对话里渲染「正在查询客户 / 已查到 N 条」等步骤。
 */
@Data
public class AiAgentTaskProgress {

    @Schema(description = "步骤序号（本轮工具调用的轮次，从 1 起）")
    private int sequence;

    @Schema(description = "工具名，如 search_customers")
    private String actionId;

    @Schema(description = "阶段，固定 tool")
    private String stage;

    @Schema(description = "running=执行中，done=完成")
    private String status;

    @Schema(description = "步骤标题，如「正在查询客户」")
    private String title;

    @Schema(description = "补充说明")
    private String description;

    @Schema(description = "明细：input=工具参数，output=结果摘要")
    private Map<String, Object> details;

    public static AiAgentTaskProgress running(int sequence, String actionId, String title, String input) {
        AiAgentTaskProgress p = new AiAgentTaskProgress();
        p.setSequence(sequence);
        p.setActionId(actionId);
        p.setStage("tool");
        p.setStatus("running");
        p.setTitle(title);
        p.setDetails(Map.of("input", input == null ? "" : input));
        return p;
    }

    public static AiAgentTaskProgress done(int sequence, String actionId, String title, String output) {
        AiAgentTaskProgress p = new AiAgentTaskProgress();
        p.setSequence(sequence);
        p.setActionId(actionId);
        p.setStage("tool");
        p.setStatus("done");
        p.setTitle(title);
        p.setDetails(Map.of("output", output == null ? "" : output));
        return p;
    }
}
