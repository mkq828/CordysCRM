package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租户版本快照（开通时快照版本名/价格/功能集合/起止时间/状态）
 */
@Data
@Table(name = "tenant_edition")
public class TenantEdition extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "版本编码")
    private String editionCode;

    @Schema(description = "版本名称(快照)")
    private String editionName;

    @Schema(description = "成交价(快照)")
    private BigDecimal price;

    @Schema(description = "功能集合JSON(快照)")
    private String featuresJson;

    @Schema(description = "AI月度配额快照(标准次数)")
    private Integer aiMonthlyQuota;

    @Schema(description = "开通时间(毫秒)")
    private Long startTime;

    @Schema(description = "到期时间(毫秒)")
    private Long expireTime;

    @Schema(description = "状态(ACTIVE/EXPIRED)")
    private String status;
}
