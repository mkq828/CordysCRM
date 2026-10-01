package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 续费/升级申请分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantPlanApplicationPageRequest extends BasePageRequest {

    @Schema(description = "状态(PENDING/APPROVED/CANCELLED)")
    private String status;

    @Schema(description = "关键字(租户名)")
    private String keyword;
}
