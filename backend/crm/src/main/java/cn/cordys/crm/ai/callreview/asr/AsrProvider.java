package cn.cordys.crm.ai.callreview.asr;

/**
 * 语音转写 Provider：与对话模型（{@link cn.cordys.crm.ai.llm.LlmProvider}）分离，
 * 因为协议不同（ASR 是异步提交 + 轮询，非 SSE 流式）。
 */
public interface AsrProvider {

    /** provider 名，与 agent_model.provider 的「阿里云」等取值对齐 */
    String name();

    /**
     * 提交转写任务，返回任务 ID（异步）。
     *
     * @param fileUrl 录音文件地址：公网 http(s) URL、data: base64 URL，或平台临时上传得到的 oss:// 地址
     * @param apiKey  平台 API Key
     */
    String submit(String fileUrl, String apiKey) throws Exception;

    /**
     * 将本地录音字节转成可直接提交转写的 file_url：小文件走 data: base64 URL，大文件走平台临时上传（oss://）。
     * 阿里云 paraformer 不支持直接本地文件上传，小文件支持 data: URL（约 7MB 以内），
     * 更大的文件需先经 getPolicy 上传到平台临时 OSS 再提交。
     *
     * @param bytes    录音字节
     * @param fileName 原始文件名（用于上传时保留扩展名）
     * @param mimeType 音频 MIME 类型（如 audio/wav、audio/mpeg）
     * @param apiKey   平台 API Key
     */
    String resolveLocalFileUrl(byte[] bytes, String fileName, String mimeType, String apiKey) throws Exception;

    /**
     * 轮询转写任务：完成时返回转写文本，未完成返回 {@code null}，失败抛异常。
     */
    String poll(String taskId, String apiKey) throws Exception;
}
