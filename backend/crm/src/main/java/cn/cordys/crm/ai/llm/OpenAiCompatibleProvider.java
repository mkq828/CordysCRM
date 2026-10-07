package cn.cordys.crm.ai.llm;

import cn.cordys.common.util.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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
import java.util.Base64;
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

    /** 文生图同步出图较慢（Seedream 5.0 pro 实测约 2 分钟），单独放宽到 5 分钟，避免被对话超时截断 */
    private static final Duration IMAGE_REQUEST_TIMEOUT = Duration.ofMinutes(5);

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
        if (request.getThinking() != null && !request.getThinking().isEmpty()) {
            body.put("thinking", request.getThinking());
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

    @Override
    public byte[] generateImage(LlmImageRequest request) throws Exception {
        String url = request.getBaseUrl() + "/images/generations";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel());
        body.put("prompt", request.getPrompt());
        body.put("size", request.getSize());
        body.put("response_format", "url");
        body.put("watermark", false);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(IMAGE_REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + request.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("{} 文生图失败，status={}, body={}", name(), response.statusCode(), response.body());
            throw new RuntimeException(name() + " 文生图失败(" + response.statusCode() + ")");
        }

        JsonNode data = JSON.MAPPER.readTree(response.body()).path("data");
        if (data.isArray() && !data.isEmpty()) {
            String imageUrl = data.get(0).path("url").asText(null);
            if (StringUtils.isNotBlank(imageUrl)) {
                return downloadImage(imageUrl);
            }
            String b64 = data.get(0).path("b64_json").asText(null);
            if (StringUtils.isNotBlank(b64)) {
                return Base64.getDecoder().decode(b64);
            }
        }
        throw new RuntimeException(name() + " 文生图未返回图片: " + response.body());
    }

    /** 下载文生图返回的临时 URL（已签名，通常 24 小时内有效，需及时下载） */
    private byte[] downloadImage(String imageUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("图片下载失败(" + response.statusCode() + ")");
        }
        return response.body();
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
