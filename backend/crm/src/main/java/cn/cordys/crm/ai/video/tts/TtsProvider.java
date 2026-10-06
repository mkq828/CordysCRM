package cn.cordys.crm.ai.video.tts;

import java.util.List;

/**
 * 语音合成（TTS）Provider：与对话模型（{@link cn.cordys.crm.ai.llm.LlmProvider}）、
 * 语音转写（{@link cn.cordys.crm.ai.callreview.asr.AsrProvider}）分离，因为协议不同（TTS 是同步 HTTP）。
 * 模型/接口/key 均来自「模型设置」里 admin 配置的配音模型，Provider 本身只实现协议，便于换服务商只改配置。
 */
public interface TtsProvider {

    /** provider 名，与 agent_model.provider 的「阿里云」「豆包语音」等取值对齐 */
    String name();

    /**
     * 把一段文本合成为语音，返回音频字节（wav）。
     *
     * @param text   要合成的文本
     * @param voice  音色标识（见 {@link #voices()}）；实现可忽略或兜底默认音色
     * @param config 合成配置（模型/API Key/扩展参数，来自模型设置）
     */
    byte[] synthesize(String text, String voice, TtsConfig config) throws Exception;

    /** 该服务商支持的音色列表（前端下拉用） */
    List<TtsVoice> voices();
}
