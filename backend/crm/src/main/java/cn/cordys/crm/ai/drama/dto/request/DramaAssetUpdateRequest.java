package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新漫剧资产请求。
 */
@Data
public class DramaAssetUpdateRequest {

    @Schema(description = "资产名")
    private String name;

    @Schema(description = "角色音色标识(仅 character 用，空则配音时回退默认音色)")
    private String voice;

    @Schema(description = "更细类型")
    private String assetType;

    @Schema(description = "外观描述/附加信息 JSON")
    private String paramsJson;
}
