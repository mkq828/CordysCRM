package cn.cordys.crm.platform.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 平台收款账号：我方公司各收款方式的收款账户（每种方式一条），新增回款时自动带出
 */
@Data
@Table(name = "platform_bank_account")
public class PlatformBankAccount extends BaseModel {

    @Schema(description = "收款方式：TRANSFER对公转账/ALIPAY支付宝/WECHAT微信/OFFLINE线下")
    private String accountType;

    @Schema(description = "户名")
    private String accountName;

    @Schema(description = "账号")
    private String accountNo;

    @Schema(description = "开户行/平台")
    private String bankName;
}
