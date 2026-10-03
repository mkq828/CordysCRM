package cn.cordys.crm.ai.callreview.asr;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ASR Provider 分发：按 provider 名路由到对应实现（与 {@code LlmProviderFactory} 同构）。
 */
@Component
public class AsrProviderFactory {

    private final Map<String, AsrProvider> providers;

    public AsrProviderFactory(List<AsrProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(AsrProvider::name, Function.identity()));
    }

    public AsrProvider get(String name) {
        AsrProvider provider = providers.get(name);
        if (provider == null) {
            throw new IllegalArgumentException("不支持的语音转写 provider: " + name);
        }
        return provider;
    }
}
