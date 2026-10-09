package cn.cordys.crm.ai.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 额度包购买/充值单：租户申请 → 客服/admin 确认到账（第一版不做在线支付）。
 */
@Data
@Table(name = "ai_quota_pack_order")
public class AiQuotaPackOrder extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "额度包ID")
    private String packId;

    @Schema(description = "AI 功能编码(下单时快照)")
    private String featureCode;

    @Schema(description = "额度包名称(下单时快照)")
    private String packName;

    @Schema(description = "购买次数")
    private Integer calls;

    @Schema(description = "金额(分)")
    private Long amountFen;

    @Schema(description = "PENDING待确认/CONFIRMED已到账/CANCELLED已取消")
    private String status;

    @Schema(description = "申请人")
    private String applyUser;

    @Schema(description = "申请时间(毫秒)")
    private Long applyTime;

    @Schema(description = "确认人")
    private String confirmUser;

    @Schema(description = "确认时间(毫秒)")
    private Long confirmTime;

    @Schema(description = "备注/取消原因")
    private String remark;
}
