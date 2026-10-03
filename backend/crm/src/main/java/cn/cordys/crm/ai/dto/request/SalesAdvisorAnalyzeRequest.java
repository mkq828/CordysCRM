package cn.cordys.crm.ai.dto.request;

import lombok.Data;

import java.util.List;

/**
 * AI 销售会话军师分析请求：粘贴聊天文本 + 可选截图附件 id（截图走 OCR 识别后并入分析）。
 */
@Data
public class SalesAdvisorAnalyzeRequest {

    /** 粘贴的聊天记录文本 */
    private String message;

    /** 截图附件 id（临时附件，可空） */
    private List<String> picIds;

    /** 会话 ID（可空，用于流式对话记录关联；为空时服务端新建会话） */
    private String conversationId;

    /** 关联客户 ID（可空；分析完成后把结论沉淀到 ai_analysis_result，供客户画像回读） */
    private String customerId;
}
