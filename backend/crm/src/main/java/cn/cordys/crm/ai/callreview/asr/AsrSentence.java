package cn.cordys.crm.ai.callreview.asr;

/**
 * 带时间戳的转写句子（paraformer sentences[] 一条）。
 *
 * @param text    句子文本
 * @param beginMs 起始时间（毫秒）
 * @param endMs   结束时间（毫秒，闭区间不严格，相邻句以此衔接）
 */
public record AsrSentence(String text, long beginMs, long endMs) {
}
