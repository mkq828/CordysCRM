package cn.cordys.crm.ai.knowledge.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 企业知识库设置请求（出处片段长度）。
 */
@Data
public class AiKnowledgeConfigRequest {

    @Schema(description = "出处片段最大长度（字）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer snippetMax;
}
