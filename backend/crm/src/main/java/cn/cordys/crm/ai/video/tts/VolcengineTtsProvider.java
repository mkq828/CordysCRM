package cn.cordys.crm.ai.video.tts;

import cn.cordys.common.util.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 火山引擎豆包语音 TTS（seed-tts-2.0，功能 7 配音）。
 * 新版控制台鉴权用单头 {@code X-Api-Key}（旧版 AppID/Access Token 已废弃），
 * {@code X-Api-Resource-Id} 指定模型版本（seed-tts-2.0），{@code X-Api-Request-Id} 为客户端请求 ID。
 * 协议：POST https://openspeech.bytedance.com/api/v3/tts/unidirectional/sse，SSE 逐块返回 base64 音频。
 * 相比阿里云 qwen3-tts-flash，seed-tts-2.0 情感表现力更强，并支持音色克隆（后续数字人复用）。
 */
@Slf4j
@Component
public class VolcengineTtsProvider implements TtsProvider {

    private static final String BASE_URL = "https://openspeech.bytedance.com/api/v3";
    private static final String DEFAULT_MODEL = "seed-tts-2.0";
    private static final String DEFAULT_VOICE = "zh_female_vv_uranus_bigtts";
    private static final ObjectMapper MAPPER = JSON.MAPPER;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String name() {
        return "豆包语音";
    }

    @Override
    public List<TtsVoice> voices() {
        // 对齐 printfilm 音色库（seed-tts-2.0 openspeech 预置音色），6 女声 + 4 男声
        return List.of(
                new TtsVoice("zh_female_cancan_uranus_bigtts", "灿灿 · 女声旁白", "female"),
                new TtsVoice("zh_female_tianmeixiaoyuan_uranus_bigtts", "甜美女声 · 故事", "female"),
                new TtsVoice("zh_female_shuangkuaisisi_uranus_bigtts", "爽快女声 · 都市", "female"),
                new TtsVoice("zh_female_vv_uranus_bigtts", "Vivi · 国风女声", "female"),
                new TtsVoice("zh_female_xiaohe_uranus_bigtts", "小何 · 通用女声", "female"),
                new TtsVoice("zh_male_shaonianzixin_uranus_bigtts", "少年梓辛 · 男声", "male"),
                new TtsVoice("zh_male_m191_uranus_bigtts", "云舟 · 稳重男声", "male"),
                new TtsVoice("zh_male_taocheng_uranus_bigtts", "小天 · 年轻男声", "male"),
                new TtsVoice("zh_male_ruyayichen_uranus_bigtts", "儒雅逸辰 · 男声", "male"),
                new TtsVoice("zh_male_baqiqingshu_uranus_bigtts", "霸气青叔 · 男声", "male"));
    }

    @Override
    public byte[] synthesize(String text, String voice, TtsConfig config) throws Exception {
        String model = StringUtils.defaultIfBlank(config.getModel(), DEFAULT_MODEL);
        String apiKey = config.getApiKey();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", Map.of("uid", "cordys-crm"));
        Map<String, Object> reqParams = new LinkedHashMap<>();
        reqParams.put("text", text);
        reqParams.put("speaker", StringUtils.defaultIfBlank(voice, DEFAULT_VOICE));
        Map<String, Object> audioParams = new LinkedHashMap<>();
        audioParams.put("format", "wav");
        audioParams.put("sample_rate", 24000);
        reqParams.put("audio_params", audioParams);
        // 保留原始符号（不吞标点），让口播断句更自然
        reqParams.put("additions", "{\"disable_markdown_filter\":true}");
        body.put("req_params", reqParams);

        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/tts/unidirectional/sse"))
                .timeout(Duration.ofMinutes(2))
                .header("X-Api-Key", apiKey)
                .header("X-Api-Resource-Id", model)
                .header("X-Api-Request-Id", UUID.randomUUID().toString())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("豆包语音 TTS 合成失败，status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("豆包语音合成失败(" + response.statusCode() + ")");
        }
        return parseSseAudio(response.body());
    }

    /** 解析 SSE：逐行 {@code data: {json}}，每个分片独立 base64 解码后按字节拼接为 WAV */
    private byte[] parseSseAudio(String body) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String lastError = null;
        for (String line : body.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            String payload = trimmed.substring(5).trim();
            if (payload.isEmpty() || "[DONE]".equals(payload)) {
                continue;
            }
            JsonNode node;
            try {
                node = MAPPER.readTree(payload);
            } catch (Exception e) {
                continue; // 非 JSON 行（如注释），跳过
            }
            String code = node.path("code").asText("");
            if (!"0".equals(code) && !"20000000".equals(code)) {
                lastError = node.path("message").asText("未知错误");
                continue;
            }
            String audio = extractAudio(node);
            if (StringUtils.isNotBlank(audio)) {
                // 每个 SSE data 分片都是独立 base64 编码，各自带 = 填充；
                // 必须逐片解码成字节再拼接，不能先拼 base64 字符串再一次性解码（会撞上中间分片的 = 填充）。
                out.write(Base64.getDecoder().decode(audio));
            }
        }
        if (out.size() == 0) {
            throw new RuntimeException("豆包语音未返回音频" + (lastError == null ? "" : ": " + lastError));
        }
        return out.toByteArray();
    }

    /** data 字段可能是 base64 字符串，也可能是对象（内嵌 audio/payload 字段），兼容两种返回 */
    private String extractAudio(JsonNode node) {
        JsonNode data = node.get("data");
        if (data == null || data.isNull()) {
            return null;
        }
        if (data.isTextual()) {
            return data.asText();
        }
        String audio = data.path("audio").asText(null);
        if (StringUtils.isNotBlank(audio)) {
            return audio;
        }
        String payload = data.path("payload").asText(null);
        return StringUtils.isNotBlank(payload) ? payload : null;
    }
}
