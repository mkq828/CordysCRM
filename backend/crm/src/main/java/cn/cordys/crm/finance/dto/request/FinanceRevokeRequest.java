package cn.cordys.crm.finance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 回款核销撤回请求
 */
@Data
public class FinanceRevokeRequest {

    @Schema(description = "回款记录ID")
    @NotBlank
    private String id;

    @Schema(description = "撤回备注")
    private String remark;
}
