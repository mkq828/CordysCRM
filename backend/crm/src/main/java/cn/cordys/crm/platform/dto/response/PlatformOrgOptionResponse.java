package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台侧租户（组织）下拉选项
 */
@Data
public class PlatformOrgOptionResponse {

    @Schema(description = "组织ID")
    private String id;

    @Schema(description = "组织名称")
    private String name;

    @Schema(description = "组织类型(PERSONAL/ENTERPRISE)")
    private String orgType;

    @Schema(description = "租户当前套餐版本code(用于新建合同时默认带出)")
    private String editionCode;

    @Schema(description = "是否已有有效合同(草稿/作废不计)，用于首份合同用首年价、续约用年价")
    private Boolean hasContract;
}
