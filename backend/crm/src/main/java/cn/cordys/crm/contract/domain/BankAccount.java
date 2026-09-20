package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "bank_account")
public class BankAccount extends BaseModel {

    @Schema(description = "账户名称")
    private String name;

    @Schema(description = "收款方式")
    private String type;

    @Schema(description = "开户行")
    private String openingBank;

    @Schema(description = "银行账号")
    private String bankAccount;

    @Schema(description = "户名")
    private String accountHolder;

    @Schema(description = "收款二维码")
    private String qrcode;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "组织id")
    private String organizationId;
}
