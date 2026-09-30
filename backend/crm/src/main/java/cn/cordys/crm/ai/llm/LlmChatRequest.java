package cn.cordys.crm.ai.llm;

import lombok.Data;

import java.util.List;

/**
 * 一次模型对话请求。baseUrl / apiKey 由 {@link cn.cordys.crm.ai.model.service.AgentModelService} 按租户模型配置注入，
 * Provider 实现类无状态，随请求取连接信息。
 */
@Data
public class LlmChatRequest {

    /** 模型编码，如 qwen-plus */
    private String model;

    /** Provider 的 OpenAI 兼容根地址，如 https://dashscope.aliyuncs.com/compatible-mode/v1 */
    private String baseUrl;

    /** Provider 的访问密钥 */
    private String apiKey;

    private List<LlmMessage> messages;
}
