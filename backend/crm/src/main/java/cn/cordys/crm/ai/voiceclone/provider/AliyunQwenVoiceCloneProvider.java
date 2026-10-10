package cn.cordys.crm.ai.voiceclone.provider;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AliyunVoiceCloneConstant;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阿里云百炼 Qwen-Audio-TTS 声音复刻（功能 11）。与现有「阿里云」provider 共用同一把百炼 API Key。
 * 协议：POST /api/v1/services/audio/tts/customization，{@code model=voice-enrollment}、{@code action=create_voice}，
 * 参考音频经平台临时存储上传（getPolicy → OSS multipart → oss:// URL）传入 {@code url} 参数，
 * 同步返回 {@code output.voice_id}（永久可用），无需训练轮询。
 * 后续合成需用复刻时配置的模型（默认 {@link AliyunVoiceCloneConstant#QWEN_TTS_VC_MODEL}）+ voice_id 作为 voice 参数。
 */
@Slf4j
@Component
public class AliyunQwenVoiceCloneProvider implements VoiceCloneProvider {

    private static final String BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final ObjectMapper MAPPER = JSON.MAPPER;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String name() {
        return "阿里云";
    }

    @Override
    public String clone(byte[] audio, String format, String preferredName, String model, String apiKey) throws Exception {
        String ext = resolveExtension(format);
        String ossUrl = uploadToOss(audio, ext, model, apiKey);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("action", "create_voice");
        input.put("target_model", model);
        input.put("prefix", preferredName);
        input.put("url", ossUrl);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", AliyunVoiceCloneConstant.QWEN_VOICE_ENROLLMENT_MODEL);
        body.put("input", input);

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(BASE_URL + "/services/audio/tts/customization"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                // oss:// 临时地址需显式开启解析，否则复刻接口无法读取参考音频
                .header("X-DashScope-OssResourceResolve", "enable")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("阿里云声音复刻提交失败，status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("阿里云声音复刻提交失败(" + response.statusCode() + ")");
        }

        JsonNode root = MAPPER.readTree(response.body());
        String voiceId = root.path("output").path("voice_id").asText(null);
        if (StringUtils.isBlank(voiceId)) {
            log.error("阿里云声音复刻未返回 voice_id，body={}", response.body());
            throw new GenericException("声音复刻失败，请稍后重试");
        }
        return voiceId;
    }

    /** 参考音频格式白名单（阿里声音复刻支持 wav/mp3/m4a），返回文件扩展名 */
    private String resolveExtension(String format) {
        String f = StringUtils.defaultIfBlank(format, "wav").toLowerCase();
        if ("wav".equals(f)) {
            return "wav";
        }
        if ("mp3".equals(f) || "mpeg".equals(f)) {
            return "mp3";
        }
        if ("m4a".equals(f) || "mp4".equals(f)) {
            return "m4a";
        }
        throw new GenericException("请使用 wav/mp3/m4a 格式的样本音频");
    }

    /**
     * 参考音频两步上传：先 getPolicy 拿临时 OSS 上传凭据，再 multipart POST 上传，返回可供复刻接口消费的 oss:// 地址。
     * 逻辑与 {@code AliyunParaformerProvider.uploadToOss} 一致，此处按声音复刻的目标模型独立实现，避免牵动 ASR。
     */
    private String uploadToOss(byte[] bytes, String ext, String model, String apiKey) throws Exception {
        HttpRequest policyRequest = HttpRequest.newBuilder(
                        URI.create(BASE_URL + "/uploads?action=getPolicy&model=" + model))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();
        HttpResponse<String> policyResponse = httpClient.send(policyRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (policyResponse.statusCode() < 200 || policyResponse.statusCode() >= 300) {
            log.error("阿里云 getPolicy 失败，status={}, body={}", policyResponse.statusCode(), policyResponse.body());
            throw new RuntimeException("阿里云声音复刻上传失败(" + policyResponse.statusCode() + ")");
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
            throw new RuntimeException("阿里云声音复刻上传凭据缺失");
        }

        String safeName = "sample_" + System.currentTimeMillis() + "." + ext;
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
            throw new RuntimeException("阿里云声音复刻上传失败(" + uploadResponse.statusCode() + ")");
        }
        return "oss://" + objectKey;
    }

    private static final String BOUNDARY = "----CordysVoiceClone" + System.currentTimeMillis();

    /**
     * 组装 OSS POST 表单 multipart body。OSS 的 key 必须是 getPolicy 返回的 upload_dir 前缀下的完整对象路径
     * （policy 里 starts-with 条件约束了 key 前缀），而 file 部分必须是最后一个字段。
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
}
