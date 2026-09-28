package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 城市经理租户归属分配请求
 */
@Data
public class CityManagerAssignRequest {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "签约城市经理ID(永久，可空)")
    private String signManagerId;

    @Schema(description = "跟进城市经理ID(可转移)")
    private String followManagerId;
}
