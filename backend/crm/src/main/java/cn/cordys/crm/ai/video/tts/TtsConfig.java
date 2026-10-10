package cn.cordys.crm.ai.video.tts;

import cn.cordys.crm.ai.volcengine.VolcengineVoiceCredential;
import lombok.Data;

/**
 * TTS 语音合成配置：由「模型设置」里 admin 配置的配音模型（agent_model）解析而来。
 * 让 TTS 换服务商/换模型/换 key 都走配置（模型路由策略 + 模型设置），不用改代码。
 */
@Data
public class TtsConfig {

    /** 模型 ID（如 qwen3-tts-flash / seed-tts-2.0），留空时用 provider 内置默认 */
    private String model;

    /** 平台 API Key（阿里云等单串 key 服务商使用；豆包语音用 {@link #volcCredential}） */
    private String apiKey;

    /** 豆包语音三件套凭据（AppID/AccessToken/SecretKey），provider=豆包语音时使用 */
    private VolcengineVoiceCredential volcCredential;

    /** 扩展参数（JSON，可配语速/音调等），留空用默认 */
    private String modelParams;

    /** 声音克隆模型名（阿里云声音复刻 target_model，如 qwen-audio-3.1-tts-flash），克隆音色合成时用 */
    private String voiceCloneModel;
}
