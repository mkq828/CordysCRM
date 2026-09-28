package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台发票开票/作废请求
 */
@Data
public class PlatformInvoiceActionRequest {

    @Schema(description = "发票ID")
    private String id;

    @Schema(description = "发票号(开票时回填)")
    private String invoiceNo;
}
