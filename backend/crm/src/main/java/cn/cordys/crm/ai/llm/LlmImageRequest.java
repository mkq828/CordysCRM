package cn.cordys.crm.ai.llm;

import lombok.Data;

/**
 * 文生图请求（OpenAI 兼容 {@code /images/generations} 协议，如豆包方舟 Seedream）。
 */
@Data
public class LlmImageRequest {

    private String model;
    private String baseUrl;
    private String apiKey;
    private String prompt;
    /** 输出尺寸，如 1080x1920 竖屏 */
    private String size = "1080x1920";
}
