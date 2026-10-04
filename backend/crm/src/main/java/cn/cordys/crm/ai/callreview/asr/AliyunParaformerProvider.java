package cn.cordys.crm.ai.callreview.asr;

import cn.cordys.common.util.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(BASE_URL + "/services/audio/asr/transcription"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-DashScope-Async", "enable");
        if (fileUrl != null && fileUrl.startsWith("oss://")) {
            requestBuilder.header("X-DashScope-OssResourceResolve", "enable");
        }
        HttpRequest request = requestBuilder
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

    /** data: base64 URL 上限约 7MB，保守取 6MB 以内走 data: URL，更大的走平台临时上传 */
    private static final int DATA_URL_MAX_BYTES = 6 * 1024 * 1024;

    @Override
    public String resolveLocalFileUrl(byte[] bytes, String fileName, String mimeType, String apiKey) throws Exception {
        if (bytes.length <= DATA_URL_MAX_BYTES) {
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
        return uploadToOss(bytes, fileName, apiKey);
    }

    /**
     * 大文件两步上传：先 getPolicy 拿临时 OSS 上传凭据，再 multipart POST 上传，返回可提交转写的 oss:// 地址。
     */
    private String uploadToOss(byte[] bytes, String fileName, String apiKey) throws Exception {
        HttpRequest policyRequest = HttpRequest.newBuilder(URI.create(BASE_URL + "/uploads?action=getPolicy&model=" + MODEL))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();
        HttpResponse<String> policyResponse = httpClient.send(policyRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (policyResponse.statusCode() < 200 || policyResponse.statusCode() >= 300) {
            log.error("阿里云 getPolicy 失败，status={}, body={}", policyResponse.statusCode(), policyResponse.body());
            throw new RuntimeException("阿里云语音转写上传失败(" + policyResponse.statusCode() + ")");
        }
        JsonNode data = MAPPER.readTree(policyResponse.body()).path("data");
        String uploadHost = data.path("upload_host").asText(null);
        String uploadDir = data.path("upload_dir").asText(null);
        String policy = data.path("policy").asText(null);
        String signature = data.path("signature").asText(null);
        String ossAccessKeyId = data.path("oss_access_key_id").asText(null);
        String xOssObjectAcl = data.path("x_oss_object_acl").asText("private");
        String xOssForbidOverwrite = data.path("x_oss_forbid_overwrite").asText("true");
        if (StringUtils.isAnyBlank(uploadHost, uploadDir, policy, signature, ossAccessKeyId)) {
            log.error("阿里云 getPolicy 返回字段缺失: {}", policyResponse.body());
            throw new RuntimeException("阿里云语音转写上传凭据缺失");
        }

        String safeName = sanitizeFileName(fileName);
        String objectKey = uploadDir + "/" + safeName;
        byte[] body = buildOssMultipartBody(bytes, objectKey, safeName, policy, signature, ossAccessKeyId, xOssObjectAcl, xOssForbidOverwrite);
        HttpRequest uploadRequest = HttpRequest.newBuilder(URI.create(uploadHost))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> uploadResponse = httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (uploadResponse.statusCode() < 200 || uploadResponse.statusCode() >= 300) {
            log.error("阿里云 OSS 上传失败，status={}, body={}", uploadResponse.statusCode(), uploadResponse.body());
            throw new RuntimeException("阿里云语音转写上传失败(" + uploadResponse.statusCode() + ")");
        }
        return "oss://" + objectKey;
    }

    private static final String BOUNDARY = "----CordysAsr" + System.currentTimeMillis();

    /**
     * 组装 OSS POST 表单 multipart body。OSS 的 key 必须是 getPolicy 返回的 upload_dir 前缀下的完整对象路径
     * （policy 里 starts-with 条件约束了 key 前缀），而 file 部分的 filename 只用文件名本身。
     */
    private byte[] buildOssMultipartBody(byte[] fileBytes, String objectKey, String fileName, String policy, String signature,
                                         String ossAccessKeyId, String xOssObjectAcl, String xOssForbidOverwrite) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeField(out, "key", objectKey);
            writeField(out, "policy", policy);
            writeField(out, "OSSAccessKeyId", ossAccessKeyId);
            writeField(out, "signature", signature);
            writeField(out, "x-oss-object-acl", xOssObjectAcl);
            writeField(out, "x-oss-forbid-overwrite", xOssForbidOverwrite);

            String fileHeader = "--" + BOUNDARY + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n";
            out.write(fileHeader.getBytes(StandardCharsets.UTF_8));
            out.write(fileBytes);
            out.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        }
    }

    private void writeField(ByteArrayOutputStream out, String name, String value) throws IOException {
        String field = "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
        out.write(field.getBytes(StandardCharsets.UTF_8));
    }

    /** 只保留文件名安全字符（扩展名、数字、字母、下划线、连字符），避免 OSS 上传目录注入 */
    private String sanitizeFileName(String fileName) {
        String base = StringUtils.defaultIfBlank(fileName, "recording");
        String cleaned = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (StringUtils.isBlank(cleaned) || ".".equals(cleaned) || "..".equals(cleaned)) {
            cleaned = "recording";
        }
        return cleaned;
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
