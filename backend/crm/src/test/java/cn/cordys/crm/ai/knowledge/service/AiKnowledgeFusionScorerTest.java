package cn.cordys.crm.ai.knowledge.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 融合排序打分器回归测试，锁定「弱词误命中不得压过强语义」与「多词强命中应胜出」两条排序口径。
 */
class AiKnowledgeFusionScorerTest {

    private static final double STRONG_SIM = 0.85;
    private static final double MID_SIM = 0.6;

    @Test
    void strongSemanticBeatsWeakLexicalHit() {
        // 提问「退费怎么处理」的真实 2-gram 词表：字面「退费」命中不了内容里的「退款」，只能靠语义
        List<String> keywords = List.of("退费", "费怎", "怎么", "么处", "处理");

        // 退款块：语义强相关，但无任何字面命中（sim 来自向量召回）
        double refundScore = AiKnowledgeFusionScorer.fusionScore(
                "客户如需退款，请在 7 天内申请，全额退款至原支付账户。", keywords, STRONG_SIM);
        // 价格块：仅命中弱词「处理」，且无向量命中（sim=0，纯关键词召回）
        double priceScore = AiKnowledgeFusionScorer.fusionScore(
                "标准价格 1200 元/年，含账务处理流程。", keywords, 0.0);

        // 退款块命中率 0、sim 0.85 → 0.85；价格块命中率 1/5、sim 0 → 0.4
        assertEquals(0.85, refundScore, 1e-9);
        assertEquals(0.4, priceScore, 1e-9);
        assertTrue(refundScore > priceScore, "强语义块应压过弱词误命中的块");
    }

    @Test
    void multipleLexicalHitsBeatSemanticTie() {
        // 提问「代理记账多少钱」的真实 2-gram 词表
        List<String> keywords = List.of("代理", "理记", "记账", "账多", "多少", "少钱");

        // 价格块：字面命中「代理/理记/记账」三处，sim 中等
        double priceScore = AiKnowledgeFusionScorer.fusionScore(
                "代理记账标准价 1200 元/年。", keywords, MID_SIM);
        // 无关块：无字面命中，sim 相同
        double otherScore = AiKnowledgeFusionScorer.fusionScore(
                "我们的签约流程分为三步。", keywords, MID_SIM);

        // 价格块命中率 3/6、sim 0.6 → 1.6；无关块命中率 0、sim 0.6 → 0.6
        assertEquals(1.6, priceScore, 1e-9);
        assertEquals(0.6, otherScore, 1e-9);
        assertTrue(priceScore > otherScore, "多词强命中的块应胜出");
    }

    @Test
    void emptyKeywordsFallsBackToSimilarity() {
        // 无关键词时命中率为 0，综合分退化为纯相似度
        assertEquals(0.7, AiKnowledgeFusionScorer.fusionScore("任意内容", List.of(), 0.7), 1e-9);
    }

    @Test
    void countHitsCountsContainedKeywordsAndIsNullSafe() {
        assertEquals(3, AiKnowledgeFusionScorer.countHits(
                "代理记账多少钱", List.of("代理", "记账", "多少")));
        assertEquals(0, AiKnowledgeFusionScorer.countHits(
                "退款政策", List.of("退费", "处理")));
        assertEquals(0, AiKnowledgeFusionScorer.countHits(null, List.of("退费")));
        assertEquals(0, AiKnowledgeFusionScorer.countHits("内容", List.of()));
    }
}
