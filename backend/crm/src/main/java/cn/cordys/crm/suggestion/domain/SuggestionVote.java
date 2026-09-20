package cn.cordys.crm.suggestion.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 建议投票（一人一票）。
 */
@Data
@Table(name = "suggestion_vote")
public class SuggestionVote extends BaseModel {

    @Schema(description = "建议ID")
    private String suggestionId;

    @Schema(description = "用户ID")
    private String userId;
}
