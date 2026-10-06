package cn.cordys.crm.ai.video.tts;

import cn.cordys.common.util.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

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

/**
 * 阿里云百炼 TTS 语音合成（功能 7 TTS）。
 * 复用租户「阿里云」模型的同一把百炼 API Key（与通话复盘 ASR 的 paraformer-v2 同账号）。
 * 协议参考 DashScope 语音合成：POST /api/v1/services/aigc/multimodal-generation/generation。
 * 注意：经典 cosyvoice-v1 的 HTTP 合成要求账号开通专用端点，普通百炼 key 会报「不支持 http call」，
 * 故改用同账号可直接调用的 qwen3-tts-flash（返回 WAV）。
 */
@Slf4j
@Component
public class AliyunCosyVoiceTtsProvider implements TtsProvider {

    private static final String BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final String DEFAULT_MODEL = "qwen3-tts-flash";
    private static final String DEFAULT_VOICE = "Cherry";
    private static final ObjectMapper MAPPER = JSON.MAPPER;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String name() {
        return "阿里云";
    }

    @Override
    public List<TtsVoice> voices() {
        return List.of(
                new TtsVoice("Cherry", "芊悦（女声·亲切自然）"),
                new TtsVoice("Serena", "苏瑶（女声·温柔）"),
                new TtsVoice("Chelsie", "活力女声（甜美）"),
                new TtsVoice("Ethan", "晨煦（男声·阳光）"));
    }

    @Override
    public byte[] synthesize(String text, String voice, TtsConfig config) throws Exception {
        // 阿里云复用「阿里云」LLM 模型的同一把百炼 key，但 TTS 与 LLM 模型名不同：
        // agent_model 里存的是 LLM 模型（如 qwen-max），TTS 固定用 qwen3-tts-flash，故不取 config.model。
        String model = DEFAULT_MODEL;
        String apiKey = config.getApiKey();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", Map.of("text", text));
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("voice", StringUtils.defaultIfBlank(voice, DEFAULT_VOICE));
        parameters.put("format", "wav");
        // 情感引导：口播短视频需要更有感染力，避免平淡播报（qwen3-tts-flash 支持 instruction）
        parameters.put("instruction", "用自然、有感染力、有节奏感的口播语气播报，重点信息适当加重语气");
        body.put("parameters", parameters);

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(BASE_URL + "/services/aigc/multimodal-generation/generation"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("阿里云 TTS 合成失败，status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("阿里云语音合成失败(" + response.statusCode() + ")");
        }

        JsonNode audio = MAPPER.readTree(response.body()).path("output").path("audio");
        String url = audio.path("url").asText(null);
        if (StringUtils.isNotBlank(url)) {
            return download(url);
        }
        String data = audio.path("data").asText(null);
        if (StringUtils.isNotBlank(data)) {
            return Base64.getDecoder().decode(data);
        }
        throw new RuntimeException("阿里云语音合成未返回音频: " + response.body());
    }

    /** 下载百炼返回的临时音频地址（已签名 OSS URL，无需鉴权） */
    private byte[] download(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("阿里云语音合成音频下载失败(" + response.statusCode() + ")");
        }
        return response.body();
    }
}
