package cn.cordys.crm.ai.callreview.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通话复盘详情：通话信息 + 转写文本 + 复盘结果。
 */
@Data
public class CallReviewDetailResponse {

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

    @Schema(description = "录音公网URL")
    private String recordUrl;

    @Schema(description = "语音转写文本")
    private String transcript;

    @Schema(description = "复盘结果(结构=会话军师结构化字段)")
    private Object review;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;

    @Schema(description = "更新时间(毫秒)")
    private Long updateTime;
}
