package cn.cordys.crm.ai.llm;

import lombok.Data;

import java.util.List;
import java.util.Map;

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

    /** 采样温度（可选，来自模型配置 model_params.temperature） */
    private Double temperature;

    /** top_p 采样（可选，来自模型配置 model_params.top_p） */
    private Double topP;

    /** 最大输出 token 数（可选，来自模型配置 model_params.max_tokens） */
    private Integer maxTokens;

    /** 思考模式（可选，来自模型配置 model_params.thinking，如豆包 Seed 模型 {"type":"disabled"} 关闭深度思考提速） */
    private Map<String, Object> thinking;
}
