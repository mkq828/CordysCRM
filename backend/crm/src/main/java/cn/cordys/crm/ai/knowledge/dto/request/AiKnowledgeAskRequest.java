package cn.cordys.crm.ai.knowledge.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 企业知识库问答请求。
 */
@Data
public class AiKnowledgeAskRequest {

    @Schema(description = "问题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    @Schema(description = "引用出处数量（默认 4）")
    private Integer topK;

    @Schema(description = "会话 ID（可空，用于流式对话记录关联；为空时服务端新建会话）")
    private String conversationId;

    @Schema(description = "是否联网补充时效信息（默认关；开启后按问题联网检索公开信息注入，供知识库资料覆盖不足时补充）")
    private Boolean webSearch;
}
