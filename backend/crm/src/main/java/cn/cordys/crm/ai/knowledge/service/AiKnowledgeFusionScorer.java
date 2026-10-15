package cn.cordys.crm.ai.knowledge.service;

import java.util.List;

/**
 * 知识库检索融合排序的纯函数打分器，抽成无状态静态方法以便单元测试锁定排序口径。
 * 综合分 = 关键词命中率（0~1）×2 + 向量相似度（0~1），让「多个强命中」胜过「单个弱命中 + 中等语义」，
 * 也让「纯向量命中的强语义块（sim 高、无字面命中）」胜过「纯关键词弱词误命中的块（sim=0、少命中）」。
 */
public final class AiKnowledgeFusionScorer {

    private AiKnowledgeFusionScorer() {
    }

    /**
     * 融合排序综合分。相似度来自向量召回（未向量命中的块取 0），关键词命中率来自字面召回，
     * 两者共同决定排序，避免只按命中数排序时弱词（如「处理」）误压强语义块。
     */
    public static double fusionScore(String content, List<String> keywords, double similarity) {
        int keywordCount = keywords.size();
        double hitRate = keywordCount == 0 ? 0 : (double) countHits(content, keywords) / keywordCount;
        return hitRate * 2 + similarity;
    }

    /** 统计内容中命中的关键词个数（含即命中，重复词按去重后的关键词列表逐项判断） */
    public static int countHits(String content, List<String> keywords) {
        if (content == null) {
            return 0;
        }
        int hits = 0;
        for (String kw : keywords) {
            if (content.contains(kw)) {
                hits++;
            }
        }
        return hits;
    }
}
