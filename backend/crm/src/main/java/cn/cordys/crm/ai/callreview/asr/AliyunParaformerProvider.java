package cn.cordys.crm.ai.callreview.asr;

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
 * 阿里云百炼录音文件识别（paraformer-v2）：异步提交 + 轮询，复用租户「阿里云」模型的同一把百炼 API Key。
 * 协议参考 DashScope 录音文件识别：提交 /api/v1/services/audio/asr/transcription，轮询 /api/v1/tasks/{id}。
 */
@Slf4j
@Component
public class AliyunParaformerProvider implements AsrProvider {

    private static final String BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final String MODEL = "paraformer-v2";
    private static final ObjectMapper MAPPER = JSON.MAPPER;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String name() {
        return "阿里云";
    }

    @Override
    public String submit(String fileUrl, String apiKey) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("input", Map.of("file_urls", List.of(fileUrl)));

        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/services/audio/asr/transcription"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-DashScope-Async", "enable")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("阿里云 ASR 提交失败，status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("阿里云语音转写提交失败(" + response.statusCode() + ")");
        }

        JsonNode output = MAPPER.readTree(response.body()).get("output");
        String taskId = output == null ? null : output.path("task_id").asText(null);
        if (StringUtils.isBlank(taskId)) {
            log.error("阿里云 ASR 未返回任务ID, body={}", response.body());
            throw new RuntimeException("阿里云语音转写未返回任务ID");
        }
        return taskId;
    }

    @Override
    public String upload(byte[] bytes, String fileName, String apiKey) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("resource", Base64.getEncoder().encodeToString(bytes));
        body.put("resource_type", "audio");

        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/uploads"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-DashScope-OssResourceResolve", "enable")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("阿里云 ASR 上传失败，status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("阿里云语音转写上传失败(" + response.statusCode() + ")");
        }

        JsonNode files = MAPPER.readTree(response.body()).path("data").path("uploaded_files");
        String ossUrl = files.isArray() && !files.isEmpty() ? files.get(0).path("oss_url").asText(null) : null;
        if (StringUtils.isBlank(ossUrl)) {
            log.error("阿里云 ASR 上传未返回文件地址, body={}", response.body());
            throw new RuntimeException("阿里云语音转写上传未返回文件地址");
        }
        return ossUrl;
    }

    @Override
    public String poll(String taskId, String apiKey) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/tasks/" + taskId))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("阿里云 ASR 轮询失败，status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("阿里云语音转写查询失败(" + response.statusCode() + ")");
        }

        JsonNode output = MAPPER.readTree(response.body()).get("output");
        if (output == null) {
            throw new RuntimeException("阿里云语音转写查询返回异常: " + response.body());
        }
        String status = output.path("task_status").asText("");
        if ("SUCCEEDED".equals(status)) {
            String transcriptionUrl = output.path("results").isArray() && !output.path("results").isEmpty()
                    ? output.path("results").get(0).path("transcription_url").asText(null) : null;
            if (StringUtils.isBlank(transcriptionUrl)) {
                throw new RuntimeException("阿里云语音转写未返回结果地址");
            }
            return fetchTranscript(transcriptionUrl);
        }
        if ("FAILED".equals(status) || "ERROR".equals(status)) {
            throw new RuntimeException("阿里云语音转写失败: " + output.path("message").asText(""));
        }
        return null;
    }

    /** 拉取转写结果 JSON（OSS 公开地址，无需鉴权），取整段 text */
    private String fetchTranscript(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("阿里云语音转写结果拉取失败(" + response.statusCode() + ")");
        }
        JsonNode transcripts = MAPPER.readTree(response.body()).path("transcripts");
        if (transcripts.isArray() && !transcripts.isEmpty()) {
            return transcripts.get(0).path("text").asText("");
        }
        return "";
    }
}
