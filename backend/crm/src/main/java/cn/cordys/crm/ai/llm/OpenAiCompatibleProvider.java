package cn.cordys.crm.ai.llm;

import cn.cordys.common.util.JSON;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI 兼容协议 Provider 抽象基类（千问 / DeepSeek / 豆包共用同一套协议，仅 baseUrl 不同）。
 * <p>
 * 请求 /chat/completions，开启 stream + stream_options.include_usage，
 * 逐段回调增量文本，末尾从 usage 取 prompt_tokens / completion_tokens。
 * </p>
 */
@Slf4j
public abstract class OpenAiCompatibleProvider implements LlmProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public abstract String name();

    @Override
    @SuppressWarnings("unchecked")
    public LlmUsage chatStream(LlmChatRequest request, Consumer<String> onChunk) throws Exception {
        String url = request.getBaseUrl() + "/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel());
        body.put("messages", request.getMessages().stream()
                .map(this::buildMessage)
                .toList());
        body.put("stream", true);
        body.put("stream_options", Map.of("include_usage", true));
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + request.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            String errorBody = readAll(response.body());
            log.error("{} 调用失败，status={}, body={}", name(), statusCode, errorBody);
            throw new RuntimeException(name() + " 调用失败(" + statusCode + "): " + errorBody);
        }

        LlmUsage usage = new LlmUsage();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    continue;
                }
                try {
                    Map<String, Object> map = JSON.parseToMap(data);

                    Map<String, Object> usageMap = (Map<String, Object>) map.get("usage");
                    if (usageMap != null) {
                        usage.setInputTokens(toLong(usageMap.get("prompt_tokens")));
                        usage.setOutputTokens(toLong(usageMap.get("completion_tokens")));
                        usage.setTotalTokens(toLong(usageMap.get("total_tokens")));
                    }

                    List<Map<String, Object>> choices = (List<Map<String, Object>>) map.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> delta = (Map<String, Object>) choices.getFirst().get("delta");
                        if (delta != null && delta.get("content") != null) {
                            String content = String.valueOf(delta.get("content"));
                            if (!content.isEmpty()) {
                                onChunk.accept(content);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析流式响应失败: {}", e.getMessage());
                }
            }
        }
        return usage;
    }

    /** 构造单条消息；带图片时 content 用 OpenAI 多模态数组（text + image_url） */
    private Map<String, Object> buildMessage(LlmMessage m) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", m.getRole());
        msg.put("content", buildContent(m));
        return msg;
    }

    private Object buildContent(LlmMessage m) {
        List<String> images = m.getImageUrls();
        if (images == null || images.isEmpty()) {
            return m.getContent();
        }
        List<Map<String, Object>> parts = new ArrayList<>();
        if (m.getContent() != null && !m.getContent().isEmpty()) {
            Map<String, Object> textPart = new LinkedHashMap<>();
            textPart.put("type", "text");
            textPart.put("text", m.getContent());
            parts.add(textPart);
        }
        for (String dataUrl : images) {
            Map<String, Object> imagePart = new LinkedHashMap<>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", Map.of("url", dataUrl));
            parts.add(imagePart);
        }
        return parts;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0;
        }
        return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
    }

    private String readAll(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }
}
