package cn.cordys.crm.ai.employee.tool;

import java.util.Map;

/**
 * AI 员工「自动跑任务」只读工具（功能 6 编排层的工具能力）。
 * <p>
 * 每个实现对应一个 CRM 只读查询能力，由 {@link AiAgentToolRegistry} 收集后写进岗位 Agent 的系统提示词，
 * 供模型按约定输出工具调用 JSON 来触发。工具以「触发任务的登录用户」身份执行（{@code orgId}/{@code userId}
 * 显式传入），结果返回精简 JSON 字符串供模型下一轮推理，绝不写 CRM 数据。
 * </p>
 */
public interface AiAgentTool {

    /** 工具名（模型输出 JSON 里的 {@code tool} 字段取值，全局唯一） */
    String name();

    /** 工具用途与参数说明，写进系统提示词供模型理解何时、如何调用 */
    String description();

    /** 执行只读查询，返回精简 JSON 字符串（供模型阅读，须控制长度） */
    String execute(String orgId, String userId, Map<String, Object> args);
}
