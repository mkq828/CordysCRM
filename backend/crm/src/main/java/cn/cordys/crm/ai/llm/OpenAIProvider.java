package cn.cordys.crm.ai.llm;

import org.springframework.stereotype.Component;

/**
 * OpenAI Provider。
 */
@Component
public class OpenAIProvider extends OpenAiCompatibleProvider {

    @Override
    public String name() {
        return "OpenAI";
    }
}
