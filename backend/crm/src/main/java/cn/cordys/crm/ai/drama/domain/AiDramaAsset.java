package cn.cordys.crm.ai.drama.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 漫剧资产：角色/场景/道具/音色，角色形象做跨镜视觉锚点保证一致性。
 */
@Data
@Table(name = "ai_drama_asset")
public class AiDramaAsset extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "所属项目ID")
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

    @Schema(description = "形象生成状态: generating/done/failed")
    private String status;
}
