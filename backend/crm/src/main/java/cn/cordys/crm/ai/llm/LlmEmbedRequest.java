package cn.cordys.crm.ai.llm;

import lombok.Data;

/**
 * 文本向量化请求（OpenAI 兼容 {@code /embeddings} 协议，如阿里云 DashScope text-embedding-v3）。
 */
@Data
public class LlmEmbedRequest {

    private String model;
    private String baseUrl;
    private String apiKey;
    private String text;
}
