package cn.cordys.crm.ai.volcengine;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * 火山语音（openspeech.bytedance.com）旧版「三件套」鉴权签名工具。
 *
 * <p>旧版豆包语音应用（无单串 API Key）用 AppID + AccessToken + SecretKey 鉴权：请求头带
 * {@code X-Api-App-Key}（AppID）、{@code X-Api-Access-Key}（AccessToken），并追加一个
 * {@code Authorization: HMAC256; access_token=...; app_id=...; timestamp=...; signature=...}，
 * signature 用 SecretKey 对「方法\n路径\n查询\n时间\n请求体」做 HMAC-SHA256 后 Base64 编码。</p>
 *
 * <p>签名串规则：{@code METHOD + "\n" + path + "\n" + query + "\n" + date + "\n" + body}，
 * date 为 RFC1123 GMT（如 {@code Wed, 19 Jul 2023 07:12:32 GMT}），path 含 {@code /api/v3} 前缀。</p>
 *
 * <p>若首次真实调用报签名/鉴权错，优先核对本类的 date 格式、签名串字段顺序与 Authorization 头格式——
 * 这是三件套签名唯一需要调整的收敛点。</p>
 */
public final class VolcengineAuthUtils {

    private static final DateTimeFormatter RFC1123_GMT =
            DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

    private VolcengineAuthUtils() {
    }

    /**
     * 向 {@link HttpRequest.Builder} 写入三件套鉴权所需的全部请求头（含 Content-Type、Request-Id、Sequence）。
     *
     * @param builder    请求构造器
     * @param credential 三件套凭据（AppID/AccessToken/SecretKey，均须非空）
     * @param method     HTTP 方法，如 {@code POST}
     * @param path       完整路径（含 {@code /api/v3} 前缀），如 {@code /api/v3/tts/voice_clone}
     * @param query      查询串（无则传空串）
     * @param body       请求体 JSON 字符串（参与签名，须与最终发送体完全一致）
     * @param resourceId 服务资源 ID（如 TTS 的 {@code seed-tts-2.0}）；传 null 则不写 X-Api-Resource-Id
     */
    public static void applyThreePartAuth(
            HttpRequest.Builder builder,
            VolcengineVoiceCredential credential,
            String method,
            String path,
            String query,
            String body,
            String resourceId) {
        if (credential == null
                || StringUtils.isAnyBlank(credential.getAppId(), credential.getAccessToken(), credential.getSecretKey())) {
            throw new IllegalArgumentException("豆包语音三件套凭据不完整（AppID / AccessToken / SecretKey）");
        }
        String date = RFC1123_GMT.format(ZonedDateTime.now(ZoneOffset.UTC));
        String stringToSign = method + "\n" + path + "\n" + query + "\n" + date + "\n" + body;
        String signature = Base64.getEncoder().encodeToString(
                hmacSha256(credential.getSecretKey(), stringToSign));

        builder.header("Content-Type", "application/json")
                .header("X-Api-App-Key", credential.getAppId())
                .header("X-Api-Access-Key", credential.getAccessToken())
                .header("X-Api-Request-Id", UUID.randomUUID().toString())
                .header("X-Api-Sequence", "-1")
                .header("Authorization", "HMAC256; access_token=" + credential.getAccessToken()
                        + "; app_id=" + credential.getAppId()
                        + "; timestamp=" + date
                        + "; signature=" + signature);
        if (StringUtils.isNotBlank(resourceId)) {
            builder.header("X-Api-Resource-Id", resourceId);
        }
    }

    private static byte[] hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("计算 HMAC-SHA256 签名失败", e);
        }
    }
}
