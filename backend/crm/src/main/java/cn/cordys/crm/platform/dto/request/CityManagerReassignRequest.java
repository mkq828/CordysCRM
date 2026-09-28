package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 城市经理离职二次分配请求（只改跟进经理，不改签约经理）
 */
@Data
public class CityManagerReassignRequest {

    @Schema(description = "原跟进城市经理ID")
    private String fromManagerId;

    @Schema(description = "目标跟进城市经理ID")
    private String toManagerId;

    @Schema(description = "待转移的租户组织ID列表")
    private List<String> organizationIds;
}
