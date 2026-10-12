package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 漫剧资产响应。
 */
@Data
public class DramaAssetResponse {

    @Schema(description = "资产ID")
    private String id;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "角色/场景/道具/音色: character/scene/prop/voice")
    private String type;

    @Schema(description = "更细类型")
    private String assetType;

    @Schema(description = "资产名")
    private String name;

    @Schema(description = "角色音色标识(仅 character 用，空则配音时回退默认音色)")
    private String voice;

    @Schema(description = "形象图/封面附件ID")
    private String coverAttachmentId;

    @Schema(description = "外观描述/image_style/generation 等 JSON")
    private String paramsJson;

    @Schema(description = "形象生成状态")
    private String status;
}
