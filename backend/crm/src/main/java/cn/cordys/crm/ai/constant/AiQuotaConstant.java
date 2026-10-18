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

    // ==================== 联网搜索计量折算 ====================
    /** 1 次联网搜索折算 token 数的配置 key（DeepSeek 搜索按次计费 5 元/千次，非 token 计费，折算后复用额度链路） */
    public static final String PARAM_SEARCH_TOKENS_PER_CALL = "ai.deepseek.searchTokensPerCall";
    /** 1 次联网搜索折算 token 数默认值（≈0.1 次标准额度） */
    public static final long DEFAULT_SEARCH_TOKENS_PER_CALL = 1000L;
    /** 联网搜索在用量明细中的 model_code 占位（无真实模型，仅用于看板聚合与成本归集） */
    public static final String SEARCH_MODEL_CODE = "deepseek-search";

    // ==================== 额度状态 ====================
    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_SOFT_LIMITED = "SOFT_LIMITED";
    public static final String STATUS_HARD_LIMITED = "HARD_LIMITED";
    public static final String STATUS_RATE_LIMITED = "RATE_LIMITED";
    public static final String STATUS_CIRCUIT_BROKEN = "CIRCUIT_BROKEN";

    // ==================== 额度包订单状态 ====================
    public static final String PACK_ORDER_PENDING = "PENDING";
    public static final String PACK_ORDER_CONFIRMED = "CONFIRMED";
    public static final String PACK_ORDER_CANCELLED = "CANCELLED";

    /** 额度包 feature_code 通用占位（空串 = 全功能通用包） */
    public static final String PACK_FEATURE_ALL = "";

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

    /** AI 方案/PPT 生成（功能 5，生成型：G2 额度 + G3 版本开关，企业版） */
    public static final String AI_PPT = "ai_ppt";

    /** AI 员工（功能 6，编排层：角色提示词 + 岗位 Agent 对话，G2 额度 + G3 权限） */
    public static final String AI_EMPLOYEE = "ai_employee";

    /** 竞品洞察（功能 10，走任务模型挂 G2 额度 + G3 权限） */
    public static final String AI_LEAD_CRAWL = "lead_crawl";

    /** 私信获客画像（功能 8，采集不计量，AI 画像走任务模型挂 G2 额度 + G3 权限） */
    public static final String AI_DM_PROFILE = "dm_profile";

    /** 短视频生成（功能 7，生成型：脚本走 LLM 挂 G2 额度 + G3 权限，专业版+） */
    public static final String AI_VIDEO = "ai_video";

    /** AI 工具中心（文生图/文案改写/爆款标题/口播脚本/视频转格式，G2 额度 + G3 权限） */
    public static final String AI_TOOL = "ai_tool";

    /** AI 问数 BI（功能 12，生成型：大白话查数，语义层编译 SQL，G2 额度 + G3 版本开关，企业版） */
    public static final String AI_ASK_BI = "ai_ask_bi";

    /** 声音克隆（功能 11，能力组件：克隆指定音色供短视频/数字人/外呼复用，只挂 G3 版本开关，企业版） */
    public static final String AI_VOICE_CLONE = "ai_voice_clone";

    /** AI 漫剧（功能 7 扩展，生成型：剧本/分镜走 LLM 挂 G2 额度 + G3 权限，专业版+） */
    public static final String AI_DRAMA = "ai_drama";

    /** 爆款复刻（功能 7 扩展 · 换皮变体，生成型：变体方案走 LLM 挂 G2 额度 + G3 权限，专业版+） */
    public static final String AI_REPLICA = "ai_replica";

    /** 客户工商信息获取（企查查为主、联网兜底，联网兜底走 LLM 挂 G2 额度 + G3 权限） */
    public static final String AI_CUSTOMER_BUSINESS = "ai_customer_business";

    public static final List<String> AI_FEATURE_CODES = List.of(
            "ai_advisor", "ai_acquire", "ai_sales_rag", "ai_video", "wecom_auto_analysis",
            "ai_ppt", "lead_crawl", "dm_profile", "ai_employee", "ai_kb", "digital_human", "ai_call_review",
            "ai_tool", "ai_ask_bi", "ai_voice_clone", "ai_drama", "ai_replica", "ai_customer_business");

    private AiQuotaConstant() {
    }
}
