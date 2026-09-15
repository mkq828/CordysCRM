package cn.cordys.crm.system.dto.request;

import cn.cordys.common.util.rsa.RsaKey;
import cn.cordys.common.util.rsa.RsaUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 注册申请请求
 */
@Getter
@Setter
public class RegisterApplyRequest {

    @NotBlank(message = "{register.type.not_blank}")
    @Schema(description = "注册类型(PERSONAL/ENTERPRISE)")
    private String type;

    @NotBlank(message = "{register.name.not_blank}")
    @Size(max = 255, message = "{register.name.length_range}")
    @Schema(description = "主体名称(个人姓名/企业名称)")
    private String name;

    @NotBlank(message = "{register.phone.not_blank}")
    @Pattern(regexp = "^1\\d{10}$", message = "{register.phone.format_error}")
    @Schema(description = "手机号")
    private String phone;

    @NotBlank(message = "{password_is_null}")
    @Size(max = 256, message = "{password_length_too_long}")
    @Schema(description = "密码(RSA加密)")
    private String password;

    @NotBlank(message = "{register.id_card.not_blank}")
    @Size(max = 64, message = "{register.id_card.length_range}")
    @Schema(description = "身份证号(个人=本人/企业=法人)")
    private String idCard;

    @Schema(description = "统一社会信用代码(企业必填)")
    private String unifiedSocialCreditCode;

    @Schema(description = "法人姓名(企业必填)")
    private String legalPersonName;

    @Schema(description = "营业执照附件ID(企业必填)")
    private String businessLicenseAttachmentId;

    /**
     * 获取解密后的密码
     */
    public String getPassword() {
        try {
            RsaKey rsaKey = RsaUtils.getRsaKey();
            return RsaUtils.privateDecrypt(password, rsaKey.getPrivateKey());
        } catch (Exception e) {
            throw new RuntimeException("解密密码失败", e);
        }
    }
}
