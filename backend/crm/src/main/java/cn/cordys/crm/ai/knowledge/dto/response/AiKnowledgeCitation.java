package cn.cordys.crm.ai.knowledge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 企业知识库问答出处引用：由服务端按块编号反查出文档名与原文片段，避免模型编造。
 */
@Data
public class AiKnowledgeCitation {

    @Schema(description = "文档名")
    private String docName;

    @Schema(description = "引用的原文片段")
    private String content;
}
