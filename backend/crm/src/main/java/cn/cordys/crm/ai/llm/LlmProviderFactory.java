package cn.cordys.crm.ai.llm;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模型 Provider 分发：按配置的 provider 名路由到对应实现。
 */
@Component
public class LlmProviderFactory {

    private final Map<String, LlmProvider> providers;

    public LlmProviderFactory(List<LlmProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(LlmProvider::name, Function.identity()));
    }

    public LlmProvider get(String name) {
        LlmProvider provider = providers.get(name);
        if (provider == null) {
            throw new IllegalArgumentException("不支持的模型 provider: " + name);
        }
        return provider;
    }
}
