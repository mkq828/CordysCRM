package cn.cordys.crm.ai.llm;

import lombok.Data;

import java.util.List;

/**
 * 模型对话消息（OpenAI 兼容角色：system / user / assistant）。
 * 当 {@link #imageUrls} 非空时为多模态消息（文本 + 图片），供视觉模型直接读图。
 */
@Data
public class LlmMessage {

    private String role;

    private String content;

    /** 图片 data URL 列表（如 data:image/png;base64,...），为空表示纯文本消息 */
    private List<String> imageUrls;

    public LlmMessage() {
    }

    public LlmMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public LlmMessage(String role, String content, List<String> imageUrls) {
        this.role = role;
        this.content = content;
        this.imageUrls = imageUrls;
    }
}
