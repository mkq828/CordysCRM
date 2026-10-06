package cn.cordys.crm.ai.video.tts;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TTS Provider 分发：按 provider 名路由到对应实现（与 {@code LlmProviderFactory} / {@code AsrProviderFactory} 同构）。
 */
@Component
public class TtsProviderFactory {

    private final Map<String, TtsProvider> providers;

    public TtsProviderFactory(List<TtsProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(TtsProvider::name, Function.identity()));
    }

    public TtsProvider get(String name) {
        TtsProvider provider = providers.get(name);
        if (provider == null) {
            throw new IllegalArgumentException("不支持的语音合成 provider: " + name);
        }
        return provider;
    }
}
