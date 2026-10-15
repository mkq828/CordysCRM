package cn.cordys.crm.ai.knowledge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 企业知识库设置响应（出处片段长度、rerank 模型名）。
 */
@Data
public class AiKnowledgeConfigResponse {

    @Schema(description = "出处片段最大长度（字）")
    private Integer snippetMax;

    @Schema(description = "rerank 精排模型名，如 qwen3-rerank")
    private String rerankModel;

    public AiKnowledgeConfigResponse(Integer snippetMax, String rerankModel) {
        this.snippetMax = snippetMax;
        this.rerankModel = rerankModel;
    }
}
