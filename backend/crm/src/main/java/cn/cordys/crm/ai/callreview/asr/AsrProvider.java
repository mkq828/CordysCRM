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
     * @param fileUrl 录音文件地址（公网 http(s) URL，或上传接口返回的 oss:// 地址）
     * @param apiKey  平台 API Key
     */
    String submit(String fileUrl, String apiKey) throws Exception;

    /**
     * 上传本地录音文件，返回可用于转写的文件地址（本地无公网 URL 时的通道）。
     */
    String upload(byte[] bytes, String fileName, String apiKey) throws Exception;

    /**
     * 轮询转写任务：完成时返回转写文本，未完成返回 {@code null}，失败抛异常。
     */
    String poll(String taskId, String apiKey) throws Exception;
}
