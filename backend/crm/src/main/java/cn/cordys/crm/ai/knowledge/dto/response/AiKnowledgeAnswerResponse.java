package cn.cordys.crm.ai.knowledge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 企业知识库问答结果：回答 + 出处引用。
 */
@Data
public class AiKnowledgeAnswerResponse {

    @Schema(description = "基于知识库文档生成的回答")
    private String answer;

    @Schema(description = "出处引用（文档名 + 原文片段）")
    private List<AiKnowledgeCitation> citations;
}
