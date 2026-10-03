package cn.cordys.crm.ai.content.dto.response;

import lombok.Data;

import java.util.List;

/**
 * AI 获客内容生成结果。模型返回 JSON 解析得到；解析失败时仅回填 {@link #rawContent} 原文。
 */
@Data
public class AiContentGenerateResponse {

    /** 生成的内容列表 */
    private List<AiContentItem> contents;

    /** 结构化解析失败时的原文回退（模型未按 JSON 输出时） */
    private String rawContent;
}
