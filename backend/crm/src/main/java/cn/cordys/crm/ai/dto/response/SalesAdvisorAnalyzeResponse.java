package cn.cordys.crm.ai.dto.response;

import lombok.Data;

import java.util.List;

/**
 * AI 销售会话军师结构化分析结果。模型返回 JSON 解析得到；解析失败时仅回填 {@link #rawAnalysis} 原文。
 */
@Data
public class SalesAdvisorAnalyzeResponse {

    /** 意向评分（0-100） */
    private String intentScore;

    /** 成交信号 */
    private List<String> signals;

    /** 客户异议点 */
    private List<String> objections;

    /** 客户情绪 / 满意度 */
    private String emotion;

    /** 竞品提及 */
    private List<String> competitorMentions;

    /** 流失风险 */
    private String churnRisk;

    /** 候选跟进话术 */
    private List<String> suggestedScripts;

    /** 结构化解析失败时的原文回退（模型未按 JSON 输出时） */
    private String rawAnalysis;
}
