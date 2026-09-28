package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 城市经理视角的租户（组织）选项，含当前归属
 */
@Data
public class CityManagerOrgResponse {

    @Schema(description = "租户组织ID")
    private String id;

    @Schema(description = "租户名称")
    private String name;

    @Schema(description = "组织类型(PERSONAL/ENTERPRISE)")
    private String orgType;

    @Schema(description = "签约城市经理ID")
    private String signManagerId;

    @Schema(description = "跟进城市经理ID")
    private String followManagerId;
}
