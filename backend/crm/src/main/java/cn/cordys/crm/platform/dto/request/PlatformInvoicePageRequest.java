package cn.cordys.crm.platform.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台发票分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformInvoicePageRequest extends BasePageRequest {

    @Schema(description = "平台合同ID")
    private String contractId;

    @Schema(description = "开票状态(NOT_INVOICED/INVOICED/VOIDED)")
    private String invoiceStatus;
}
