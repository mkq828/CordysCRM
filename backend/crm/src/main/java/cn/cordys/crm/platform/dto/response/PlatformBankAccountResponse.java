package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台收款账号响应
 */
@Data
public class PlatformBankAccountResponse {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "收款方式：TRANSFER对公转账/ALIPAY支付宝/WECHAT微信/OFFLINE线下")
    private String accountType;

    @Schema(description = "户名")
    private String accountName;

    @Schema(description = "账号")
    private String accountNo;

    @Schema(description = "开户行/平台")
    private String bankName;
}
