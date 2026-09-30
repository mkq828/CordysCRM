package cn.cordys.crm.ai.llm;

import lombok.Data;

/**
 * 模型对话消息（OpenAI 兼容角色：system / user / assistant）。
 */
@Data
public class LlmMessage {

    private String role;

    private String content;

    public LlmMessage() {
    }

    public LlmMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
