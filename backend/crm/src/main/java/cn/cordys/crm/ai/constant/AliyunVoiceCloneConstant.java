package cn.cordys.crm.ai.constant;

/**
 * 阿里云百炼 Qwen-Audio-TTS 声音复刻常量（功能 11）。
 * 复刻与合成两侧共用，避免模型名 / voice_id 前缀魔法串散落；阿里版本迭代时只改这里。
 * 说明：Qwen-TTS 复刻（qwen-voice-enrollment，base64 直传）已下线，现用 Qwen-Audio-TTS 复刻
 * （voice-enrollment，参考音频走平台临时上传的 oss:// URL）。
 */
public final class AliyunVoiceCloneConstant {

    /** 复刻接口的 model（固定值，非合成模型；Qwen-Audio-TTS/CosyVoice 声音复刻） */
    public static final String QWEN_VOICE_ENROLLMENT_MODEL = "voice-enrollment";

    /** 克隆音色绑定的合成模型（复刻 target_model 与合成 model 必须一致）；实际以「模型设置」配置为准，此值为兜底默认 */
    public static final String QWEN_TTS_VC_MODEL = "qwen-audio-3.1-tts-flash";

    private AliyunVoiceCloneConstant() {
    }
}
