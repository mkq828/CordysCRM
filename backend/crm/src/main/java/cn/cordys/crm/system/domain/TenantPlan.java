package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 租户套餐（付费用户）
 * <p>
 * 每个租户一条记录，记录套餐版本、状态与到期时间。
 * </p>
 */
@Data
@Table(name = "tenant_plan")
public class TenantPlan extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "套餐版本(BASIC/PRO/ENTERPRISE)")
    private String version;

    @Schema(description = "状态(FREE/ACTIVE/EXPIRED)")
    private String status;

    @Schema(description = "到期时间(毫秒)")
    private Long expireTime;

    @Schema(description = "备注")
    private String remark;
}
