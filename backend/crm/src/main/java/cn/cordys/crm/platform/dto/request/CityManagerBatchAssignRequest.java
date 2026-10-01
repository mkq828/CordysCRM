package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 城市经理批量分配归属请求：一次把多家租户的签约/跟进经理批量调整（离职可一次分给多个在职合伙人）。
 */
@Data
public class CityManagerBatchAssignRequest {

    @Schema(description = "待分配条目")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "租户组织ID")
        private String organizationId;

        @Schema(description = "签约城市经理ID(永久，可空=保持不变)")
        private String signManagerId;

        @Schema(description = "跟进城市经理ID(可转移，可空=保持不变)")
        private String followManagerId;
    }
}
