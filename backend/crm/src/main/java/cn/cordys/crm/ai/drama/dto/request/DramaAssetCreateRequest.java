package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新建漫剧资产请求。
 */
@Data
public class DramaAssetCreateRequest {

    @NotBlank(message = "项目ID不能为空")
    @Schema(description = "所属项目ID")
    private String projectId;

    @NotBlank(message = "资产类型不能为空")
    @Schema(description = "角色/场景/道具/音色: character/scene/prop/voice")
    private String type;

    @Schema(description = "更细类型")
    private String assetType;

    @Schema(description = "资产名")
    private String name;

    @Schema(description = "外观描述/附加信息 JSON")
    private String paramsJson;
}
