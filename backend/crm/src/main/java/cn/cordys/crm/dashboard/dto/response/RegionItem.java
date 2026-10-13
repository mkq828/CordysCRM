package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 区域分布项（省级行政区划代码 + 数量）。code 为 6 位省级 adcode，前端据 GeoJSON 映射省名。
 */
@Data
public class RegionItem {

    @Schema(description = "省级行政区划代码（6 位 adcode，如 440000 广东）")
    private String code;

    @Schema(description = "数量")
    private long value;
}
