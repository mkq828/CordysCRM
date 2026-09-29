package cn.cordys.crm.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台端 AI 额度-租户额度行
 */
@Data
public class AiTenantQuotaRow {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "租户名称")
    private String organizationName;

    @Schema(description = "组织类型(PERSONAL/ENTERPRISE)")
    private String orgType;

    @Schema(description = "套餐版本编码")
    private String editionCode;

    @Schema(description = "套餐版本名称(快照)")
    private String editionName;

    @Schema(description = "套餐到期时间(毫秒)")
    private Long expireTime;

    @Schema(description = "套餐快照配额(次)，无套餐为 null")
    private Integer snapshotQuota;

    @Schema(description = "手动覆盖配额(次)，未覆盖为 null")
    private Integer overrideQuota;

    @Schema(description = "生效配额(次)")
    private Integer effectiveQuota;

    @Schema(description = "本月已用标准次数")
    private BigDecimal usedCalls;

    @Schema(description = "本月剩余次数")
    private BigDecimal remainingCalls;

    @Schema(description = "额度状态(NORMAL/SOFT_LIMITED/HARD_LIMITED)")
    private String status;
}
