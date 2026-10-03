package cn.cordys.crm.ai.knowledge.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 企业知识库文档（ai_knowledge_doc 表，租户级）。租户上传的产品资料/制度/话术等文档，解析后按块存储供 AI 检索问答。
 */
@Data
@Table(name = "ai_knowledge_doc")
public class AiKnowledgeDoc extends BaseModel {

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "文档名（原始文件名）")
    private String name;

    @Schema(description = "文件类型（pdf/docx/md/txt）")
    private String fileType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "解析状态（READY/FAILED）")
    private String status;

    @Schema(description = "分块数")
    private Integer chunkCount;

    @Schema(description = "解析失败原因")
    private String errorMsg;
}
