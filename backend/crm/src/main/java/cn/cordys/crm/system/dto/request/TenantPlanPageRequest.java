package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户套餐分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantPlanPageRequest extends BasePageRequest {

    @Schema(description = "套餐版本(PERSONAL/ENTERPRISE)")
    private String version;

    @Schema(description = "状态(FREE/ACTIVE/EXPIRED)")
    private String status;

    @Schema(description = "关键字(组织名称/管理员手机号)")
    private String keyword;
}
