package cn.cordys.crm.ai.voiceclone.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 声音克隆 Provider 分发：按 provider 名路由到对应实现（与 {@code TtsProviderFactory} / {@code AsrProviderFactory} 同构）。
 */
@Component
public class VoiceCloneProviderFactory {

    private final Map<String, VoiceCloneProvider> providers;

    public VoiceCloneProviderFactory(List<VoiceCloneProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(VoiceCloneProvider::name, Function.identity()));
    }

    public VoiceCloneProvider get(String name) {
        VoiceCloneProvider provider = providers.get(name);
        if (provider == null) {
            throw new IllegalArgumentException("不支持的声音克隆 provider: " + name);
        }
        return provider;
    }
}
