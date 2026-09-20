package cn.cordys.crm.suggestion.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 建议评论/回复。
 */
@Data
@Table(name = "suggestion_comment")
public class SuggestionComment extends BaseModel {

    @Schema(description = "建议ID")
    private String suggestionId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "回复评论ID")
    private String replyCommentId;
}
