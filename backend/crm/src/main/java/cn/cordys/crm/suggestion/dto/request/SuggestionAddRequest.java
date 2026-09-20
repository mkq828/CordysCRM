package cn.cordys.crm.suggestion.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SuggestionAddRequest {

    @Schema(description = "标题")
    @NotBlank(message = "{suggestion.title.not_blank}")
    @Size(max = 200, message = "{suggestion.title.length_range}")
    private String title;

    @Schema(description = "内容")
    @NotBlank(message = "{suggestion.content.not_blank}")
    private String content;

    @Schema(description = "图片临时附件ID集合")
    private List<String> imageIds;
}
