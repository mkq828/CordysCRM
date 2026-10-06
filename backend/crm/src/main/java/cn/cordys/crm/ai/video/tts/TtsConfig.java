package cn.cordys.crm.ai.video.tts;

import lombok.Data;

/**
 * TTS 语音合成配置：由「模型设置」里 admin 配置的配音模型（agent_model）解析而来。
 * 让 TTS 换服务商/换模型/换 key 都走配置（模型路由策略 + 模型设置），不用改代码。
 */
@Data
public class TtsConfig {

    /** 模型 ID（如 qwen3-tts-flash / seed-tts-2.0），留空时用 provider 内置默认 */
    private String model;

    /** 平台 API Key */
    private String apiKey;

    /** 扩展参数（JSON，可配语速/音调等），留空用默认 */
    private String modelParams;
}
