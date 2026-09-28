package cn.cordys.crm.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台端 AI 成本看板查询
 */
@Data
public class AdminAiCostRequest {

    @Schema(description = "聚合维度(DAY/MONTH，缺省 MONTH)")
    private String groupBy;

    @Schema(description = "起始时间(毫秒，缺省当月1日)")
    private Long startTime;

    @Schema(description = "结束时间(毫秒，缺省当月最后一天)")
    private Long endTime;
}
