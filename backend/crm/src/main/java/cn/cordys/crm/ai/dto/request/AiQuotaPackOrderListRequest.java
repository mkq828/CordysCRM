package cn.cordys.crm.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * admin 额度包订单列表查询
 */
@Data
public class AiQuotaPackOrderListRequest {

    @Schema(description = "关键字(额度包名称/租户ID)")
    private String keyword;

    @Schema(description = "状态(PENDING/CONFIRMED/CANCELLED)")
    private String status;
}
