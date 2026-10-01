package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 注册申请单响应（敏感字段脱敏）
 */
@Data
public class RegisterApplicationResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "注册类型(PERSONAL/ENTERPRISE)")
    private String type;

    @Schema(description = "主体名称(个人姓名/企业名称)")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "身份证号(脱敏)")
    private String idCard;

    @Schema(description = "统一社会信用代码")
    private String unifiedSocialCreditCode;

    @Schema(description = "法人姓名")
    private String legalPersonName;

    @Schema(description = "营业执照附件ID")
    private String businessLicenseAttachmentId;

    @Schema(description = "审核状态(PENDING/APPROVED/REJECTED)")
    private String verifyStatus;

    @Schema(description = "审核备注")
    private String verifyRemark;

    @Schema(description = "审核人")
    private String verifyUser;

    @Schema(description = "审核时间")
    private Long verifyTime;

    @Schema(description = "开通的用户ID")
    private String userId;

    @Schema(description = "套餐ID")
    private String planId;

    @Schema(description = "套餐版本(PERSONAL/ENTERPRISE)")
    private String planVersion;

    @Schema(description = "套餐状态(FREE/ACTIVE/EXPIRED)")
    private String planStatus;

    @Schema(description = "套餐到期时间(毫秒)")
    private Long planExpireTime;

    @Schema(description = "剩余可用天数(套餐到期日-当前日期，未开通套餐为 null)")
    private Long remainDays;

    @Schema(description = "累计使用天数(实际登录天数)")
    private Long usageDays;

    @Schema(description = "最后一次登录时间")
    private Long lastLoginTime;

    @Schema(description = "账号是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    private Long createTime;
}
