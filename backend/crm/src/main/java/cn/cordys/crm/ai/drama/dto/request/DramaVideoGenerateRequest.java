package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 生成资产/分镜图生视频请求（Seedance）。
 */
@Data
public class DramaVideoGenerateRequest {

    @NotBlank(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private String projectId;

    @NotBlank(message = "资产ID不能为空")
    @Schema(description = "资产ID（用其形象图作首帧）")
    private String assetId;

    @Schema(description = "视频提示词")
    private String prompt;

    @Schema(description = "视频模型ID")
    private String modelId;

    @Schema(description = "输出比例")
    private String aspectRatio;

    @Schema(description = "时长(秒)")
    private Integer durationSec;

    @Schema(description = "参考资产ID列表（角色一致性，预留）")
    private List<String> referenceAssetIds;
}
