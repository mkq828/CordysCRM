package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 组织（租户）
 *
 * @author jianxing
 */
@Data
@Table(name = "sys_organization")
public class Organization extends BaseModel {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "组织类型(PERSONAL/ENTERPRISE)")
    private String orgType;

    @Schema(description = "统一社会信用代码")
    private String unifiedSocialCreditCode;

    @Schema(description = "法人姓名")
    private String legalPersonName;

    @Schema(description = "法人身份证号(AES加密)")
    private String legalPersonIdCard;

    @Schema(description = "法人身份证号md5")
    private String legalPersonIdCardHash;

    @Schema(description = "营业执照附件ID")
    private String businessLicenseAttachmentId;
}
