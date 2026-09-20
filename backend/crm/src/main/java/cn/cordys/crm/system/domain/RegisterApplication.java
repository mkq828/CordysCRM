package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 注册申请单
 *
 * @author jianxing
 */
@Data
@Table(name = "sys_register_application")
public class RegisterApplication extends BaseModel {

    @Schema(description = "注册类型(PERSONAL/ENTERPRISE)")
    private String type;

    @Schema(description = "主体名称(个人姓名/企业名称)")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "密码(bcrypt)")
    private String password;

    @Schema(description = "身份证号(AES加密)")
    private String idCard;

    @Schema(description = "身份证号sha256")
    private String idCardHash;

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

    @Schema(description = "审核通过后开通的用户ID")
    private String userId;
}
