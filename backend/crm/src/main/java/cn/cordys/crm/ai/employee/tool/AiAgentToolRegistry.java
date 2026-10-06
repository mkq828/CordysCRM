package cn.cordys.crm.ai.employee.tool;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 员工只读工具注册表：Spring 自动收集所有 {@link AiAgentTool} 实现，按 {@code name()} 建索引，
 * 供 {@code AiAgentTaskService} 拼提示词与按名执行。
 */
@Component
public class AiAgentToolRegistry {

    private final Map<String, AiAgentTool> tools;

    public AiAgentToolRegistry(List<AiAgentTool> toolList) {
        this.tools = toolList.stream()
                .collect(Collectors.toMap(AiAgentTool::name, Function.identity()));
    }

    public AiAgentTool get(String name) {
        return tools.get(name);
    }

    public Collection<AiAgentTool> all() {
        return tools.values();
    }
}
