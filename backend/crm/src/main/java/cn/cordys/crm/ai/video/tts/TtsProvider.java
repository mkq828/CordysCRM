package cn.cordys.crm.ai.video.tts;

/**
 * 语音合成（TTS）Provider：与对话模型（{@link cn.cordys.crm.ai.llm.LlmProvider}）、
 * 语音转写（{@link cn.cordys.crm.ai.callreview.asr.AsrProvider}）分离，因为协议不同（TTS 是同步 HTTP）。
 */
public interface TtsProvider {

    /** provider 名，与 agent_model.provider 的「阿里云」等取值对齐 */
    String name();

    /**
     * 把一段文本合成为语音，返回音频字节（mp3）。
     *
     * @param text   要合成的文本
     * @param voice  音色标识（如 longxiaochun）；实现可忽略或兜底默认音色
     * @param apiKey 平台 API Key
     */
    byte[] synthesize(String text, String voice, String apiKey) throws Exception;
}
