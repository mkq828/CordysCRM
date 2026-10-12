package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 从剧本抽取资产（角色/场景/道具）请求。
 */
@Data
public class DramaSeedAssetsRequest {

    @NotBlank(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "是否用 AI 刷新全部资产生图提示词并重抽道具（默认仅补全新角色/场景）")
    private Boolean refreshPrompts;
}
