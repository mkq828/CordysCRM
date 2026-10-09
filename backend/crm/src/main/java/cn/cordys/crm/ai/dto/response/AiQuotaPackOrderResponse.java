package cn.cordys.crm.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 额度包购买/充值单
 */
@Data
public class AiQuotaPackOrderResponse {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "租户名称")
    private String organizationName;

    @Schema(description = "额度包名称(下单时快照)")
    private String packName;

    @Schema(description = "AI 功能编码(下单时快照)")
    private String featureCode;

    @Schema(description = "功能名称")
    private String featureName;

    @Schema(description = "购买次数")
    private Integer calls;

    @Schema(description = "金额(分)")
    private Long amountFen;

    @Schema(description = "PENDING待确认/CONFIRMED已到账/CANCELLED已取消")
    private String status;

    @Schema(description = "申请时间(毫秒)")
    private Long applyTime;

    @Schema(description = "确认时间(毫秒)")
    private Long confirmTime;

    @Schema(description = "备注/取消原因")
    private String remark;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;
}
