package cn.cordys.crm.ai.dto.response;

import cn.cordys.crm.ai.llm.LlmUsage;

/**
 * 流式调用结果：结构化解析结果 + 本次 token 用量（供会话落库记账）。
 */
public record AiStreamResult<T>(T result, LlmUsage usage) {
}
