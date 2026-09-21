package cn.cordys.crm.finance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 回款核销请求
 */
@Data
public class FinanceVerifyRequest {

    @Schema(description = "回款记录ID")
    @NotBlank
    private String id;

    @Schema(description = "核销备注")
    private String remark;

    @Schema(description = "收款证明附件ID集合")
    private List<String> proofAttachmentIds;
}
