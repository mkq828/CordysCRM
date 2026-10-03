package cn.cordys.crm.ai.content.dto.request;

import lombok.Data;

/**
 * AI 获客内容生成请求：行业 + 产品/卖点 + 目标平台 + 生成条数。
 */
@Data
public class AiContentGenerateRequest {

    /** 行业，必填 */
    private String industry;

    /** 产品 / 卖点，必填 */
    private String product;

    /** 目标平台：douyin / xiaohongshu / moments，默认 douyin（抖音） */
    private String platform;

    /** 生成条数，默认 5，上限 10 */
    private Integer topicCount;

    /** 会话 ID（可空，用于流式对话记录关联；为空时服务端新建会话） */
    private String conversationId;
}
