package cn.cordys.crm.suggestion.dto.response;

import cn.cordys.crm.suggestion.domain.SuggestionComment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SuggestionCommentDTO extends SuggestionComment {

    @Schema(description = "评论人姓名")
    private String userName;

    @Schema(description = "被回复人姓名")
    private String replyUserName;
}
