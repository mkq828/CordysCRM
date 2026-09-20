package cn.cordys.crm.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BankAccountAddRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "账户名称")
    private String name;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "收款方式")
    private String type;

    @Size(max = 255)
    @Schema(description = "开户行")
    private String openingBank;

    @Size(max = 255)
    @Schema(description = "银行账号")
    private String bankAccount;

    @Size(max = 255)
    @Schema(description = "户名")
    private String accountHolder;

    @Size(max = 255)
    @Schema(description = "收款二维码")
    private String qrcode;

    @Size(max = 500)
    @Schema(description = "备注")
    private String remark;
}
