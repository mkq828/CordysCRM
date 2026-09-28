package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 功能保存请求（新增/编辑）
 */
@Data
public class FeatureSaveRequest {

    @Schema(description = "功能ID，为空时新增")
    private String id;

    @Schema(description = "功能编码")
    @NotBlank(message = "功能编码不能为空")
    private String featureCode;

    @Schema(description = "功能名称")
    @NotBlank(message = "功能名称不能为空")
    private String name;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "全局开关(1启用 0停用)")
    private Boolean enable;
}
