package cn.cordys.crm.ai.llm;

import org.springframework.stereotype.Component;

/**
 * 腾讯云混元 Provider。
 */
@Component
public class TencentProvider extends OpenAiCompatibleProvider {

    @Override
    public String name() {
        return "腾讯云";
    }
}
