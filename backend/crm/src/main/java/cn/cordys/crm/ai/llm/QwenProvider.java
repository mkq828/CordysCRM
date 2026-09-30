package cn.cordys.crm.ai.llm;

import org.springframework.stereotype.Component;

/**
 * 千问（阿里云 DashScope）Provider。
 */
@Component
public class QwenProvider extends OpenAiCompatibleProvider {

    @Override
    public String name() {
        return "阿里云";
    }
}
