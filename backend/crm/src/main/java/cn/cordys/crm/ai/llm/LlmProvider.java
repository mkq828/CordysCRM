package cn.cordys.crm.ai.llm;

import java.util.function.Consumer;

/**
 * 模型 Provider 统一接口（G1 模型可配置适配层）。
 * <p>
 * 每个模型厂商一个实现；对话走流式回调，返回 token 用量供额度层（{@code AiQuotaService}）记账。
 * 切换模型不改业务代码，只改平台「AI 模型配置」即可。
 * </p>
 */
public interface LlmProvider {

    /** provider 唯一标识，如 qwen / deepseek / doubao */
    String name();

    /**
     * 流式对话。
     *
     * @param request 对话请求（含 model / baseUrl / apiKey / messages）
     * @param onChunk 增量文本回调，逐段返回模型输出
     * @return 本次调用的 input/output token 用量
     */
    LlmUsage chatStream(LlmChatRequest request, Consumer<String> onChunk) throws Exception;
}
