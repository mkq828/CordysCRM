package cn.cordys.crm.ai.model.constant;

import cn.cordys.crm.ai.constant.AiQuotaConstant;

import java.util.List;

/**
 * AI 功能 → 模型用途路由的静态注册表，是「模型应用场景」只读矩阵的唯一真相源。
 * 功能编码复用 {@link AiQuotaConstant} 既有常量，中文名以 sys_feature.name 为准。
 * 向量化（embedding）、ASR 转写属内部基础设施，不在本矩阵单独成行。
 */
public final class AiFeatureRouteRegistry {

    /** 单个 AI 功能消费的模型用途路由 */
    public record AiFeatureRoute(String featureCode, String featureName, List<ModelRoute> routes, String remark) {
    }

    public static final List<AiFeatureRoute> FEATURES = List.of(
            new AiFeatureRoute(AiQuotaConstant.AI_CHAT, "通用 AI 对话", List.of(ModelRoute.CHAT), null),
            new AiFeatureRoute(AiQuotaConstant.AI_ADVISOR, "AI 客户军师", List.of(ModelRoute.CHAT, ModelRoute.TASK), null),
            new AiFeatureRoute(AiQuotaConstant.AI_SALES_RAG, "销售话术库", List.of(ModelRoute.CHAT), null),
            new AiFeatureRoute(AiQuotaConstant.AI_ACQUIRE, "AI 获客内容生成", List.of(ModelRoute.CHAT), null),
            new AiFeatureRoute(AiQuotaConstant.AI_KB, "企业知识库", List.of(ModelRoute.CHAT), null),
            new AiFeatureRoute(AiQuotaConstant.AI_PPT, "AI 方案/PPT 演示生成", List.of(ModelRoute.CHAT), null),
            new AiFeatureRoute(AiQuotaConstant.AI_EMPLOYEE, "AI 员工", List.of(ModelRoute.CHAT, ModelRoute.TASK), null),
            new AiFeatureRoute(AiQuotaConstant.AI_LEAD_CRAWL, "竞品洞察", List.of(ModelRoute.TASK), null),
            new AiFeatureRoute(AiQuotaConstant.AI_DM_PROFILE, "私信获客画像", List.of(ModelRoute.TASK), null),
            new AiFeatureRoute(AiQuotaConstant.AI_VIDEO, "短视频生成",
                    List.of(ModelRoute.CHAT, ModelRoute.IMAGE, ModelRoute.VIDEO, ModelRoute.TTS), null),
            new AiFeatureRoute(AiQuotaConstant.AI_TOOL, "AI 工具中心", List.of(ModelRoute.CHAT, ModelRoute.IMAGE), null),
            new AiFeatureRoute(AiQuotaConstant.AI_ASK_BI, "AI 问数 BI", List.of(ModelRoute.CHAT), null),
            new AiFeatureRoute(AiQuotaConstant.AI_VOICE_CLONE, "声音克隆", List.of(ModelRoute.VOICE_CLONE), null),
            new AiFeatureRoute(AiQuotaConstant.AI_DRAMA, "AI 漫剧",
                    List.of(ModelRoute.CHAT, ModelRoute.IMAGE, ModelRoute.VIDEO, ModelRoute.TTS), null),
            new AiFeatureRoute(AiQuotaConstant.AI_REPLICA, "爆款复刻", List.of(ModelRoute.CHAT), null),
            new AiFeatureRoute(AiQuotaConstant.AI_CALL_REVIEW, "智能通话复盘", List.of(ModelRoute.TASK), null),
            new AiFeatureRoute(AiQuotaConstant.AI_CUSTOMER_PROFILE, "客户画像分析", List.of(), "只读聚合，不调用大模型"),
            new AiFeatureRoute("wecom_auto_analysis", "企微会话自动分析", List.of(), "未上线（预留）"),
            new AiFeatureRoute("digital_human", "数字人/剪辑", List.of(), "未上线（预留）")
    );

    private AiFeatureRouteRegistry() {
    }
}
