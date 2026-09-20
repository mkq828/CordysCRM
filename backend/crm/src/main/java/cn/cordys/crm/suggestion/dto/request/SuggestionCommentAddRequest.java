package cn.cordys.crm.suggestion.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuggestionCommentAddRequest {

    @Schema(description = "建议ID")
    @NotBlank(message = "{suggestion.id.not_blank}")
    private String suggestionId;

    @Schema(description = "内容")
    @NotBlank(message = "{suggestion.content.not_blank}")
    private String content;

    @Schema(description = "回复评论ID")
    private String replyCommentId;
}
