package cn.cordys.crm.suggestion.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuggestionStatusRequest {

    @Schema(description = "建议ID")
    @NotBlank(message = "{suggestion.id.not_blank}")
    private String id;

    @Schema(description = "状态")
    @NotBlank(message = "{suggestion.status.not_blank}")
    private String status;
}
