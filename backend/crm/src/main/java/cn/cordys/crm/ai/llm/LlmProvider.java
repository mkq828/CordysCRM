package cn.cordys.crm.ai.llm;

import java.util.List;
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

    /**
     * 文生图（OpenAI 兼容 {@code /images/generations}）。仅支持文生图的 provider 覆盖实现，
     * 其余 provider 默认不支持（如 DeepSeek），调用即抛异常。
     *
     * @return 生成图片的二进制内容（jpg/png）
     */
    default byte[] generateImage(LlmImageRequest request) throws Exception {
        throw new UnsupportedOperationException("当前模型不支持文生图: " + name());
    }

    /**
     * 文本向量化（OpenAI 兼容 {@code /embeddings}）。仅支持向量化的 provider 覆盖实现，
     * 其余 provider 默认不支持，调用即抛异常。
     *
     * @return 文本向量（浮点数组）
     */
    default float[] embed(LlmEmbedRequest request) throws Exception {
        throw new UnsupportedOperationException("当前模型不支持向量化: " + name());
    }

    /**
     * 文本精排（Cohere 兼容 {@code /reranks}）。仅支持 rerank 的 provider 覆盖实现，
     * 其余 provider 默认不支持，调用即抛异常。
     *
     * @return 按相关性降序排列的原始文档下标列表
     */
    default List<Integer> rerank(LlmRerankRequest request) throws Exception {
        throw new UnsupportedOperationException("当前模型不支持 rerank 精排: " + name());
    }
}
