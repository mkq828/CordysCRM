package cn.cordys.crm.finance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 财务回款明细——付款凭证附件
 */
@Data
public class FinanceAttachmentResponse {

    @Schema(description = "附件ID")
    private String id;

    @Schema(description = "附件名称")
    private String name;

    @Schema(description = "附件类型(扩展名)")
    private String type;

    @Schema(description = "附件大小(字节)")
    private Long size;
}
