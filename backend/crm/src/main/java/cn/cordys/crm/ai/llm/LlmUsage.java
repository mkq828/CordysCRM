package cn.cordys.crm.ai.llm;

import lombok.Data;

/**
 * 模型调用 token 用量（input / output 分开，供额度层记账）。
 */
@Data
public class LlmUsage {

    private long inputTokens;

    private long outputTokens;

    private long totalTokens;
}
