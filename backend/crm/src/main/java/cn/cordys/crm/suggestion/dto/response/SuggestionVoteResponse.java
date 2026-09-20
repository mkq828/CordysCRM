package cn.cordys.crm.suggestion.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SuggestionVoteResponse {

    @Schema(description = "最新票数")
    private Integer voteCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean voted;
}
