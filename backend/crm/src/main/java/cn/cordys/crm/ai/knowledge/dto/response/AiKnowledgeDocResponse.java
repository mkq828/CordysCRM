package cn.cordys.crm.ai.knowledge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

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

    @Schema(description = "版本号（自由文本，如 V1.2）")
    private String version;

    @Schema(description = "生效日期，NULL=立即生效")
    private LocalDate effectiveDate;

    @Schema(description = "是否废止（1=已废止）")
    private Integer revoked;

    @Schema(description = "解析状态（READY/FAILED）")
    private String status;

    @Schema(description = "分块数")
    private Integer chunkCount;

    @Schema(description = "解析失败原因")
    private String errorMsg;

    @Schema(description = "排序号（列表按升序排，NULL 排最后）")
    private Integer sortNo;

    @Schema(description = "原文件附件ID，用于在线查看")
    private String attachmentId;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "创建时间")
    private Long createTime;
}
