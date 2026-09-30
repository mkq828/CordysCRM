package cn.cordys.crm.ai.llm;

import org.springframework.stereotype.Component;

/**
 * 自定义 Provider（OpenAI 兼容协议，apiUrl 由用户填写）。
 */
@Component
public class CustomProvider extends OpenAiCompatibleProvider {

    @Override
    public String name() {
        return "自定义";
    }
}
