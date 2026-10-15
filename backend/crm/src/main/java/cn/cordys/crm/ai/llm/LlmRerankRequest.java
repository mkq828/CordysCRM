package cn.cordys.crm.ai.llm;

import lombok.Data;

import java.util.List;

/**
 * 文本精排请求（Cohere 兼容 {@code /reranks} 协议，如阿里云 DashScope qwen3-rerank）。
 * 对召回候选按与 query 的相关性重排，返回按相关性降序的原始文档下标。
 */
@Data
public class LlmRerankRequest {

    private String model;
    private String baseUrl;
    private String apiKey;
    private String query;
    private List<String> documents;
    private Integer topN;
}
