package cn.cordys.crm.ai.llm;

import org.springframework.stereotype.Component;

/**
 * Anthropic Provider（走其 OpenAI 兼容端点）。
 */
@Component
public class AnthropicProvider extends OpenAiCompatibleProvider {

    @Override
    public String name() {
        return "Anthropic";
    }
}
