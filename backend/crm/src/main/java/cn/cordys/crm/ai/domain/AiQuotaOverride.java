package cn.cordys.crm.ai.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 月度额度租户覆盖（admin 按租户单独调额，覆盖套餐快照/试用配额）
 */
@Data
@Table(name = "ai_quota_override")
public class AiQuotaOverride extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "覆盖后的AI月度额度(标准次数)，0=停用AI")
    private Integer quota;
}
