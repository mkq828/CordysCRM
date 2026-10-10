package cn.cordys.crm.ai.volcengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 火山语音（openspeech）旧版「三件套」凭据：AppID + AccessToken + SecretKey。
 * 豆包语音 TTS 与声音复刻共用；SecretKey 仅用于服务端校验请求时客户端侧生成 HMAC256 签名，不随请求体回传。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolcengineVoiceCredential {

    private String appId;

    private String accessToken;

    private String secretKey;

    public static VolcengineVoiceCredential of(String appId, String accessToken, String secretKey) {
        return new VolcengineVoiceCredential(appId, accessToken, secretKey);
    }
}
