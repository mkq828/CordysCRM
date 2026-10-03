package cn.cordys.crm.customer.dto.response;

import cn.cordys.common.util.BigDecimalNoTrailingZeroSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户画像（360° 只读聚合 + AI 洞察）。查看型：只挂 feature_code 权限开关，不调模型、不计量。
 */
@Data
public class CustomerProfileResponse {

    @Schema(description = "当前租户是否可用（专业版+；未开通时前端渲染升级空态）")
    private Boolean available;

    // ==================== 谁（客户基础） ====================
    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "最新跟进时间")
    private Long followTime;

    @Schema(description = "最新跟进人名称")
    private String followerName;

    // ==================== 跟进 ====================
    @Schema(description = "跟进汇总")
    private ProfileFollowSummary follow;

    // ==================== 商机 ====================
    @Schema(description = "商机汇总（数量+金额）")
    private ProfileCountAmount opportunity;

    @Schema(description = "商机阶段分布")
    private List<ProfileOpportunityStageCount> opportunityStages;

    // ==================== 订单 / 合同 / 回款 ====================
    @Schema(description = "订单汇总（数量+金额）")
    private ProfileCountAmount order;

    @Schema(description = "合同汇总（数量+金额）")
    private ProfileCountAmount contract;

    @Schema(description = "已回款金额（已核销）")
    @JsonSerialize(using = BigDecimalNoTrailingZeroSerializer.class)
    private BigDecimal paidAmount;

    @Schema(description = "回款率（%）：已回款 / 合同总额")
    @JsonSerialize(using = BigDecimalNoTrailingZeroSerializer.class)
    private BigDecimal paymentRate;

    // ==================== AI 洞察（会话军师沉淀，回读） ====================
    @Schema(description = "最新 AI 洞察（会话军师结构化结论；无则 null）")
    private Object aiInsight;
}
