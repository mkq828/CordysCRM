package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生成资产生图请求（角色/场景/道具形象）。
 */
@Data
public class DramaImageGenerateRequest {

    @NotBlank(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "资产ID（为已有资产生成形象；空则按 prompt 新建）")
    private String assetId;

    @Schema(description = "生图提示词")
    private String prompt;

    @Schema(description = "资产名（新建时用）")
    private String name;

    @Schema(description = "资产类型: character/scene/prop（新建时用）")
    private String assetTypeKind;

    @Schema(description = "画面风格ID")
    private String imageStyleId;

    @Schema(description = "图片模型ID")
    private String modelId;

    @Schema(description = "输出比例，默认 9:16")
    private String aspectRatio;
}
