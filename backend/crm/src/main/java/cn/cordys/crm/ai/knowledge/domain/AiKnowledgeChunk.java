package cn.cordys.crm.ai.knowledge.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 企业知识库文档分块（ai_knowledge_chunk 表，租户级）。文档按约 500 字符切块，是检索问答的基本召回单位。
 */
@Data
@Table(name = "ai_knowledge_chunk")
public class AiKnowledgeChunk extends BaseModel {

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "所属文档ID")
    private String docId;

    @Schema(description = "块序号（从0起）")
    private Integer seq;

    @Schema(description = "块文本")
    private String content;
}
