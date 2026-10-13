package cn.cordys.crm.ai.replica.dto;

/**
 * 一个换皮变体方案（LLM 生成，落 ai_replica_variant）。
 *
 * @param hookText      钩子文案（开头大字钩子）
 * @param titleText     标题/主题标签文案
 * @param subtitleStyle 字幕样式标识（预设样式 key）
 * @param ratio         输出比例：9:16 / 16:9 / 1:1
 */
public record ReplicaVariantPlan(
        String hookText,
        String titleText,
        String subtitleStyle,
        String ratio) {
}
