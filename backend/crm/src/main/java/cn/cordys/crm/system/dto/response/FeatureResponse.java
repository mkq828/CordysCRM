package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 功能响应
 */
@Data
public class FeatureResponse {

    @Schema(description = "功能ID")
    private String id;

    @Schema(description = "功能编码")
    private String featureCode;

    @Schema(description = "功能名称")
    private String name;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "全局开关(1启用 0停用)")
    private Boolean enable;
}
