package cn.cordys.crm.ai.voiceclone.provider;

/**
 * 声音克隆 Provider：与对话模型、TTS、ASR 分离，协议不同（上传样本同步返回音色 ID）。
 * 模型接口 / 凭据来自「模型设置」里 admin 配置的配音模型，Provider 只实现协议。
 */
public interface VoiceCloneProvider {

    /** provider 名，与 agent_model.provider 的「阿里云」等取值对齐 */
    String name();

    /**
     * 上传样本音频，同步克隆音色并返回厂商 voice_id。
     *
     * @param audio         样本音频字节
     * @param format        音频格式（wav/mp3/m4a）
     * @param preferredName 音色命名关键词（厂商规则：仅字母数字下划线，长度受限）
     * @param model         声音克隆目标模型（如 qwen-audio-3.1-tts-flash），由模型设置配置
     * @param apiKey        平台 API Key（阿里云百炼单串 key）
     * @return 克隆音色 ID（voice_id），后续合成时作为 voice 参数传入
     */
    String clone(byte[] audio, String format, String preferredName, String model, String apiKey) throws Exception;
}
