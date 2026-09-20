package cn.cordys.crm.suggestion.dto.response;

import cn.cordys.crm.suggestion.domain.Suggestion;
import cn.cordys.crm.system.domain.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class SuggestionDTO extends Suggestion {

    @Schema(description = "提议者姓名")
    private String userName;

    @Schema(description = "组织名称")
    private String organizationName;

    @Schema(description = "评论数")
    private Long commentCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean voted;

    @Schema(description = "图片附件")
    private List<Attachment> imageList;
}
