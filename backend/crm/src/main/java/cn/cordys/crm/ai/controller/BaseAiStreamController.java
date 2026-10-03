package cn.cordys.crm.ai.controller;

import cn.cordys.crm.ai.llm.LlmUsage;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 流式接口公共底座：SSE 响应头、run/done 事件数据、会话标题截断。
 * 军师 / 获客内容 / 知识库 / 哆咪AI对话四个流式接口共用。
 */
public abstract class BaseAiStreamController {

    protected PrintWriter beginSse(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return response.getWriter();
    }

    protected Map<String, Object> runData(String conversationId, String runId, String assistantMessageId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("conversationId", conversationId);
        data.put("runId", runId);
        data.put("assistantMessageId", assistantMessageId);
        return data;
    }

    protected Map<String, Object> doneData(String conversationId, String runId, String assistantMessageId,
                                           LlmUsage usage, Object payload) {
        Map<String, Object> data = runData(conversationId, runId, assistantMessageId);
        data.put("totalTokens", usage.getTotalTokens());
        data.put("input", usage.getInputTokens());
        data.put("output", usage.getOutputTokens());
        data.put("payload", payload);
        return data;
    }

    /** 会话标题：取用户输入前 20 字，空则回退特性默认标题 */
    protected String title(String text, String fallback) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return fallback;
        }
        return t.length() > 20 ? t.substring(0, 20) : t;
    }
}
