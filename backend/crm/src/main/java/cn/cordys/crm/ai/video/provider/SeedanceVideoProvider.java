package cn.cordys.crm.ai.video.provider;

import cn.cordys.common.util.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 火山方舟 Ark 的 Seedance 图生视频（content-generation 异步任务：创建 → 轮询 → 下载）。
 * 与豆包 Seedream 文生图同账号同 endpoint，直接复用 agent_model 里已配置的 api_url / api_key，
 * 无需额外在「模型设置」之外再加服务商配置。
 */
@Slf4j
@Component
public class SeedanceVideoProvider {

    /** 单次创建/查询/下载的 HTTP 超时 */
    private static final Duration HTTP_TIMEOUT = Duration.ofMinutes(5);

    /** 轮询间隔与总超时（Seedance 单镜 5s 通常 1~3 分钟） */
    private static final long POLL_INTERVAL_MS = 5_000;
    private static final long MAX_POLL_MS = 10 * 60 * 1000;

    /** 单镜成片时长（秒），Seedance 支持 5 / 10；取 5 省 tokens，合成时再对齐旁白时长 */
    private static final int CLIP_DURATION = 5;

    private static final String DEFAULT_BASE = "https://ark.cn-beijing.volces.com/api/v3";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 一张配图 + 一段画面描述 → 一段 Seedance 视频，落盘到 outFile。
     * 异常直接抛出，由调用方决定回退 Ken Burns 静图成片。
     */
    public File generate(String apiUrl, String apiKey, String model, String prompt, File imageFile,
                         String ratio, File outFile) throws Exception {
        String base = apiUrl == null || apiUrl.isBlank()
                ? DEFAULT_BASE : apiUrl.replaceAll("/+$", "");
        String taskId = createTask(base, apiKey, model, prompt, imageFile, ratio);
        String videoUrl = poll(base, apiKey, taskId);
        download(videoUrl, outFile);
        return outFile;
    }

    /** 创建异步生成任务，返回任务 id（cgt-...） */
    private String createTask(String base, String apiKey, String model, String prompt,
                              File imageFile, String ratio) throws Exception {
        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);

        Map<String, Object> imageUrl = new LinkedHashMap<>();
        imageUrl.put("url", dataUri(imageFile));
        Map<String, Object> imagePart = new LinkedHashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrl);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("content", List.of(textPart, imagePart));
        body.put("duration", CLIP_DURATION);
        body.put("resolution", "720p");
        body.put("ratio", ratio);
        body.put("watermark", false);
        body.put("generate_audio", false);

        JsonNode resp = post(base + "/contents/generations/tasks", apiKey, body);
        String id = resp.path("id").asText(null);
        if (id == null || id.isBlank()) {
            throw new RuntimeException("Seedance 任务创建失败，未返回任务 id: " + resp);
        }
        return id;
    }

    /** 轮询任务直到 succeeded，返回 content.video_url；failed/expired 或超时抛异常 */
    private String poll(String base, String apiKey, String taskId) throws Exception {
        long deadline = System.currentTimeMillis() + MAX_POLL_MS;
        while (System.currentTimeMillis() < deadline) {
            JsonNode resp = get(base + "/contents/generations/tasks/" + taskId, apiKey);
            String status = resp.path("status").asText("");
            if ("succeeded".equals(status)) {
                String url = resp.path("content").path("video_url").asText(null);
                if (url == null || url.isBlank()) {
                    throw new RuntimeException("Seedance 任务成功但未返回 video_url: " + resp);
                }
                return url;
            }
            if ("failed".equals(status) || "expired".equals(status)) {
                throw new RuntimeException("Seedance 任务失败 status=" + status + ": " + resp);
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new RuntimeException("Seedance 任务超时未完成 taskId=" + taskId);
    }

    /** 下载视频到本地 mp4 */
    private void download(String url, File outFile) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<byte[]> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 300 || resp.body() == null || resp.body().length == 0) {
            throw new RuntimeException("Seedance 视频下载失败 http=" + resp.statusCode());
        }
        Files.write(outFile.toPath(), resp.body());
    }

    private JsonNode post(String url, String apiKey, Map<String, Object> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = JSON.MAPPER.readTree(resp.body());
        if (resp.statusCode() >= 300 || node.has("error")) {
            throw new RuntimeException("Seedance 接口返回异常 http=" + resp.statusCode() + " body=" + resp.body());
        }
        return node;
    }

    private JsonNode get(String url, String apiKey) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = JSON.MAPPER.readTree(resp.body());
        if (resp.statusCode() >= 300) {
            throw new RuntimeException("Seedance 查询异常 http=" + resp.statusCode() + " body=" + resp.body());
        }
        return node;
    }

    /** 配图 → base64 data URI（方舟要求小写 data:image/png;base64,...） */
    private String dataUri(File imageFile) throws Exception {
        byte[] bytes = Files.readAllBytes(imageFile.toPath());
        String mime = "png".equals(ext(imageFile.getName())) ? "image/png" : "image/jpeg";
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private String ext(String name) {
        if (name == null) {
            return "png";
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "png";
    }
}
