package cn.cordys.crm.ai.knowledge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 企业知识库文档列表/详情响应。
 */
@Data
public class AiKnowledgeDocResponse {

    @Schema(description = "文档ID")
    private String id;

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

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "创建时间")
    private Long createTime;
}
