package cn.cordys.crm.ai.attribution.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 经营归因请求：针对某个维度下钻数据生成「为什么」结论。
 */
@Data
@Schema(description = "经营归因请求")
public class AttributionRequest {

    @Schema(description = "维度类型: overview/sales/region/stage")
    private String kind;

    @Schema(description = "维度键(sales=ownerId / region=adcode / stage=阶段编码，overview 可空)")
    private String key;

    @Schema(description = "时间范围编码 today/week/month/quarter/year/all，缺省累计")
    private String range;
}
