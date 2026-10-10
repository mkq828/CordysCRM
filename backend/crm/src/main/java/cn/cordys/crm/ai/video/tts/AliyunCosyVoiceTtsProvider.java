package cn.cordys.crm.ai.video.tts;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AliyunVoiceCloneConstant;
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
 * 系统音色走 qwen3-tts-flash（multimodal-generation 端点）；克隆音色走 Qwen-Audio-TTS
 * （SpeechSynthesizer 端点 + voice 参数），合成模型名由「模型设置」里的声音克隆模型名配置。
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
                new TtsVoice("Cherry", "芊悦（女声·亲切自然）", "female"),
                new TtsVoice("Serena", "苏瑶（女声·温柔）", "female"),
                new TtsVoice("Chelsie", "活力女声（甜美）", "female"),
                new TtsVoice("Ethan", "晨煦（男声·阳光）", "male"));
    }

    @Override
    public byte[] synthesize(String text, String voice, TtsConfig config) throws Exception {
        // 阿里云复用「阿里云」LLM 模型的同一把百炼 key，但 TTS 与 LLM 模型名不同：
        // agent_model 里存的是 LLM 模型（如 qwen-max），系统音色固定用 qwen3-tts-flash，故不取 config.model。
        String apiKey = config.getApiKey();

        // 声音复刻音色（voice_id 以「复刻模型名-」开头）：合成模型必须与复刻 target_model 一致，
        // 走 SpeechSynthesizer 端点（Qwen-Audio-TTS 协议），与系统音色的 multimodal-generation 端点不同。
        String cloneModel = StringUtils.defaultIfBlank(config.getVoiceCloneModel(), AliyunVoiceCloneConstant.QWEN_TTS_VC_MODEL);
        boolean cloneVoice = voice != null && voice.startsWith(cloneModel + "-");

        HttpRequest request = cloneVoice
                ? buildCloneRequest(text, voice, cloneModel, apiKey)
                : buildSystemRequest(text, voice, apiKey);

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

    /** 克隆音色合成：Qwen-Audio-TTS 协议走 SpeechSynthesizer 端点，voice 放 input、format 放 parameters */
    private HttpRequest buildCloneRequest(String text, String voice, String model, String apiKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("text", text);
        input.put("voice", voice);
        body.put("input", input);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("format", "wav");
        body.put("parameters", parameters);
        return HttpRequest.newBuilder(URI.create(BASE_URL + "/services/audio/tts/SpeechSynthesizer"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();
    }

    /** 系统音色合成：qwen3-tts-flash 走 multimodal-generation 端点，voice 放 parameters */
    private HttpRequest buildSystemRequest(String text, String voice, String apiKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", DEFAULT_MODEL);
        body.put("input", Map.of("text", text));
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("voice", StringUtils.defaultIfBlank(voice, DEFAULT_VOICE));
        parameters.put("format", "wav");
        // 情感引导：口播短视频需要更有感染力，避免平淡播报（qwen3-tts-flash 支持 instruction）
        parameters.put("instruction", "用自然、有感染力、有节奏感的口播语气播报，重点信息适当加重语气");
        body.put("parameters", parameters);
        return HttpRequest.newBuilder(URI.create(BASE_URL + "/services/aigc/multimodal-generation/generation"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();
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
