package cn.cordys.crm.ai.model.constant;

/**
 * AI 模型用途路由，用于「模型应用场景」只读矩阵标注每个功能消费哪类模型。
 * 与 {@link AgentModelService} 的 resolve* 方法一一对应。
 */
public enum ModelRoute {

    /** 对话/通用文本 */
    CHAT("对话"),

    /** 洞察与评估任务（会话军师等单次分析场景） */
    TASK("洞察/任务"),

    /** 文生图 */
    IMAGE("文生图"),

    /** 图生视频 */
    VIDEO("文生视频"),

    /** 配音（短视频口播） */
    TTS("配音"),

    /** 声音克隆音色（阿里云） */
    VOICE_CLONE("声音克隆");

    private final String label;

    ModelRoute(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
