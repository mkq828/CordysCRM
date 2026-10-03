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

    private boolean fenced = false;

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
        if (visible.length() > forwardedLength) {
            onChunk.accept(visible.substring(forwardedLength));
            forwardedLength = visible.length();
        }
        if (fence >= 0) {
            fenced = true;
        }
    }

    /** 累积的模型全文（含叙述与 JSON），供结构化解析 */
    public String text() {
        return full.toString();
    }
}
