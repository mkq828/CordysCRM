package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 转化漏斗。一期做「各阶段对象计数」简化漏斗，不做严格逐级转化关联。
 */
@Data
public class Funnel {

    @Schema(description = "线索数")
    private long clue;

    @Schema(description = "客户数")
    private long customer;

    @Schema(description = "商机数")
    private long opportunity;

    @Schema(description = "订单数")
    private long order;

    @Schema(description = "合同数")
    private long contract;
}
