package cn.cordys.crm.ai.service;

import java.util.function.Consumer;

/**
 * 流式累积器：把模型输出累积成全文，同时只把「叙述部分」（首个 ``` 代码块之前）转发给前端，
 * 避免把结构化 JSON 逐字暴露在打字机里。适用于「叙述 + fenced JSON」两段式输出；纯文本输出时天然转发全文。
 */
public class FencedJsonStreamer {

    private final StringBuilder full = new StringBuilder();

    private final Consumer<String> onChunk;

    private int forwardedLength = 0;

    /** 引导语开头常见客套话，转发前剥掉，避免「好的，」「没问题」出现在打字机里 */
    private static final String[] LEADING_FILLERS = {"好的，", "好的", "没问题，", "没问题", "OK，", "OK", "收到，", "收到"};

    private boolean fenced = false;

    /** 已剥掉的前导客套话长度；-1 表示首段内容尚未到齐（仍可能是客套话前缀） */
    private int prefixOffset = -1;

    public FencedJsonStreamer(Consumer<String> onChunk) {
        this.onChunk = onChunk;
    }

    public void accept(String chunk) {
        full.append(chunk);
        if (fenced) {
            return;
        }
        int fence = full.indexOf("```");
        String visible = fence < 0 ? full.toString() : full.substring(0, fence);
        if (prefixOffset < 0) {
            if (isPartialFiller(visible)) {
                return;
            }
            prefixOffset = stripLeadingFiller(visible);
        }
        int start = Math.max(forwardedLength, prefixOffset);
        if (visible.length() > start) {
            onChunk.accept(visible.substring(start));
            forwardedLength = visible.length();
        }
        if (fence >= 0) {
            fenced = true;
        }
    }

    /** 当前文本仍是某个客套话的前缀（如只输出「好」），需等待更多输出 */
    private static boolean isPartialFiller(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (String filler : LEADING_FILLERS) {
            if (filler.startsWith(text)) {
                return true;
            }
        }
        return false;
    }

    /** 反复剥离开头客套话，返回被剥掉的长度 */
    private static int stripLeadingFiller(String text) {
        int offset = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String filler : LEADING_FILLERS) {
                if (text.startsWith(filler, offset)) {
                    offset += filler.length();
                    changed = true;
                    break;
                }
            }
        }
        return offset;
    }

    /** 累积的模型全文（含叙述与 JSON），供结构化解析 */
    public String text() {
        return full.toString();
    }
}
