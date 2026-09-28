package cn.cordys.crm.platform.dto.response;

import cn.cordys.crm.platform.domain.PlatformPaymentRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 平台回款响应（含合同编号/租户名/付款凭证/收款证明）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformPaymentRecordResponse extends PlatformPaymentRecord {

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "租户名称")
    private String orgName;

    @Schema(description = "付款凭证列表")
    private List<PlatformAttachmentResponse> voucherList;

    @Schema(description = "收款证明列表")
    private List<PlatformAttachmentResponse> proofList;

    @Schema(description = "签约经理姓名")
    private String signManagerName;

    @Schema(description = "跟进经理姓名")
    private String followManagerName;
}
