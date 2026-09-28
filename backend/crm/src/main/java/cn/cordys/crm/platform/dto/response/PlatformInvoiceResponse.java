package cn.cordys.crm.platform.dto.response;

import cn.cordys.crm.platform.domain.PlatformInvoice;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台发票响应（含合同编号/租户名）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformInvoiceResponse extends PlatformInvoice {

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "租户名称")
    private String orgName;

    @Schema(description = "签约经理姓名")
    private String signManagerName;

    @Schema(description = "跟进经理姓名")
    private String followManagerName;
}
