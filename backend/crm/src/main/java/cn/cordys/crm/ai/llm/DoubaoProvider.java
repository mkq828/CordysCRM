package cn.cordys.crm.ai.llm;

import org.springframework.stereotype.Component;

/**
 * 豆包（字节跳动火山方舟）Provider，走 OpenAI 兼容协议。
 */
@Component
public class DoubaoProvider extends OpenAiCompatibleProvider {

    @Override
    public String name() {
        return "豆包";
    }
}
