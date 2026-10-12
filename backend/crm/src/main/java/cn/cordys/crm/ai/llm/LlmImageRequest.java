package cn.cordys.crm.ai.llm;

import java.util.List;

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
    /** 参考图（图生图）：图片 URL 或 base64 data URI，可单张或多张；为空即纯文生图 */
    private List<String> image;
}
