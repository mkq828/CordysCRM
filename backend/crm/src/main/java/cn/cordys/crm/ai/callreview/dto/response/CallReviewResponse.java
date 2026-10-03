package cn.cordys.crm.ai.callreview.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通话复盘列表项。
 */
@Data
public class CallReviewResponse {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "外呼供应商名")
    private String supplier;

    @Schema(description = "主叫号码")
    private String caller;

    @Schema(description = "被叫号码")
    private String callee;

    @Schema(description = "客户号码")
    private String customerPhone;

    @Schema(description = "关联客户ID")
    private String customerId;

    @Schema(description = "关联客户名")
    private String customerName;

    @Schema(description = "通话时间(毫秒)")
    private Long callTime;

    @Schema(description = "通话时长(秒)")
    private Integer duration;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;
}
