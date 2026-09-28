package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台收款主体配置响应
 */
@Data
public class PlatformConfigResponse {

    @Schema(description = "公司全称")
    private String companyName;

    @Schema(description = "开票抬头")
    private String invoiceTitle;

    @Schema(description = "默认税率(%)")
    private String taxRate;
}
