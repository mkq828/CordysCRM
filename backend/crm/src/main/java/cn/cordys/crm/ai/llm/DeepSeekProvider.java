package cn.cordys.crm.ai.llm;

import org.springframework.stereotype.Component;

/**
 * DeepSeek Provider。
 */
@Component
public class DeepSeekProvider extends OpenAiCompatibleProvider {

    @Override
    public String name() {
        return "DeepSeek";
    }
}
