package cn.cordys.crm.ai.constant;

import java.util.List;

/**
 * AI 额度框架常量：配置 key、默认值、额度状态、AI 功能编码。
 */
public final class AiQuotaConstant {

    // ==================== 配置 key（sys_parameter） ====================
    /** 1 次标准调用折算的 token 数（默认 10000） */
    public static final String PARAM_TOKENS_PER_CALL = "ai.quota.tokensPerCall";
    /** 软超阈值（百分比，100 表示恰好用完即软超；默认 110 表示用到 110% 才硬超） */
    public static final String PARAM_SOFT_LIMIT_PERCENT = "ai.quota.softLimitPercent";
    /** 试用租户月配额（无版本快照时回退，默认 20） */
    public static final String PARAM_TRIAL_QUOTA = "ai.quota.trialQuota";
    /** 单租户每分钟调用上限（默认 60） */
    public static final String PARAM_MINUTE_CALL_LIMIT = "ai.quota.minuteCallLimit";
    /** 单租户每日调用上限（默认 1000） */
    public static final String PARAM_DAILY_CALL_LIMIT = "ai.quota.dailyCallLimit";
    /** 单租户单日成本熔断阈值（元，默认 100） */
    public static final String PARAM_DAILY_COST_THRESHOLD = "ai.quota.dailyCostThreshold";

    // ==================== 默认值 ====================
    public static final long DEFAULT_TOKENS_PER_CALL = 10000L;
    public static final int DEFAULT_SOFT_LIMIT_PERCENT = 110;
    public static final int DEFAULT_TRIAL_QUOTA = 20;
    public static final int DEFAULT_MINUTE_CALL_LIMIT = 60;
    public static final int DEFAULT_DAILY_CALL_LIMIT = 1000;
    public static final double DEFAULT_DAILY_COST_THRESHOLD = 100.0;

    // ==================== 额度状态 ====================
    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_SOFT_LIMITED = "SOFT_LIMITED";
    public static final String STATUS_HARD_LIMITED = "HARD_LIMITED";
    public static final String STATUS_RATE_LIMITED = "RATE_LIMITED";
    public static final String STATUS_CIRCUIT_BROKEN = "CIRCUIT_BROKEN";

    // ==================== 用量明细状态 ====================
    public static final String RECORD_SUCCESS = "SUCCESS";

    // ==================== 聚合维度 ====================
    public static final String GROUP_BY_DAY = "DAY";
    public static final String GROUP_BY_MONTH = "MONTH";

    // ==================== AI 功能编码（与 sys_feature.feature_code 一致） ====================
    /** 通用 AI 对话（G1 模型层联调用；功能 1 军师等落地后改用各自 feature_code） */
    public static final String AI_CHAT = "ai_chat";

    /** AI 销售会话军师（功能 1） */
    public static final String AI_ADVISOR = "ai_advisor";

    /** 销售话术库 RAG 检索（功能 2） */
    public static final String AI_SALES_RAG = "ai_sales_rag";

    /** AI 获客内容生成（功能 3） */
    public static final String AI_ACQUIRE = "ai_acquire";

    /** 企业知识库检索问答（功能 4） */
    public static final String AI_KB = "ai_kb";

    /** 客户画像分析（功能 13，查看型：只挂 G3 权限开关，不挂 G2 额度） */
    public static final String AI_CUSTOMER_PROFILE = "ai_customer_profile";

    /** 智能通话复盘（功能 9，录音转写不挂 G2 额度，AI 复盘挂 G2 额度 + G3 权限） */
    public static final String AI_CALL_REVIEW = "ai_call_review";

    public static final List<String> AI_FEATURE_CODES = List.of(
            "ai_advisor", "ai_acquire", "ai_sales_rag", "ai_video", "wecom_auto_analysis",
            "ai_ppt", "lead_crawl", "dm_profile", "ai_employee", "ai_kb", "digital_human", "ai_call_review");

    private AiQuotaConstant() {
    }
}
