package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 租户详情（付费用户详情页：组织信息 + 管理员 + 当前套餐 + AI 配额 + 开通/续费/升级历史）
 */
@Data
public class TenantPlanDetailResponse {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "组织类型(PERSONAL/ENTERPRISE)")
    private String orgType;

    @Schema(description = "统一社会信用代码")
    private String unifiedSocialCreditCode;

    @Schema(description = "法人姓名")
    private String legalPersonName;

    @Schema(description = "是否演示租户")
    private Boolean demo;

    @Schema(description = "管理员姓名")
    private String adminName;

    @Schema(description = "管理员手机号")
    private String phone;

    @Schema(description = "账号是否启用")
    private Boolean enabled;

    @Schema(description = "套餐版本(BASIC/PRO/ENTERPRISE)")
    private String version;

    @Schema(description = "版本名称")
    private String editionName;

    @Schema(description = "状态(FREE/ACTIVE/EXPIRED)")
    private String status;

    @Schema(description = "成交价(元)")
    private BigDecimal price;

    @Schema(description = "到期时间(毫秒)")
    private Long expireTime;

    @Schema(description = "剩余天数(负数表示已到期)")
    private Long remainingDays;

    @Schema(description = "AI 月度生效配额(标准次数)")
    private Integer aiQuota;

    @Schema(description = "AI 本月已用(标准次数)")
    private BigDecimal aiUsedCalls;

    @Schema(description = "开通/续费/升级历史")
    private List<TenantPlanHistoryResponse> histories;
}
