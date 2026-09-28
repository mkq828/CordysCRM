package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台收款账号保存请求（按收款方式 upsert）
 */
@Data
public class PlatformBankAccountSaveRequest {

    @Schema(description = "收款方式：TRANSFER对公转账/ALIPAY支付宝/WECHAT微信/OFFLINE线下")
    private String accountType;

    @Schema(description = "户名")
    private String accountName;

    @Schema(description = "账号")
    private String accountNo;

    @Schema(description = "开户行/平台")
    private String bankName;
}
