package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 城市经理编辑本人归属租户基本信息请求（不含归属经理/演示标记，归属仅 admin 可改）
 */
@Data
public class CityManagerOrgEditRequest {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "企业名称")
    private String name;

    @Schema(description = "统一社会信用代码")
    private String unifiedSocialCreditCode;

    @Schema(description = "法人姓名")
    private String legalPersonName;

    @Schema(description = "营业执照附件ID")
    private String businessLicenseAttachmentId;
}
