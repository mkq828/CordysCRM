package cn.cordys.crm.suggestion.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 需求反馈（平台全局，不按租户隔离）。
 */
@Data
@Table(name = "suggestion")
public class Suggestion extends BaseModel {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "提议者用户ID")
    private String userId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "图片附件ID(JSON数组)")
    private String imageIds;

    @Schema(description = "票数")
    private Integer voteCount;
}
