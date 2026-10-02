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
}
