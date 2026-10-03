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
}
