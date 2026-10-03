package cn.cordys.crm.ai.knowledge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 企业知识库设置响应（出处片段长度）。
 */
@Data
public class AiKnowledgeConfigResponse {

    @Schema(description = "出处片段最大长度（字）")
    private Integer snippetMax;

    public AiKnowledgeConfigResponse(Integer snippetMax) {
        this.snippetMax = snippetMax;
    }
}
