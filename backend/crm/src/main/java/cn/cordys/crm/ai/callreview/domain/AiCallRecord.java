package cn.cordys.crm.ai.callreview.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 智能通话复盘（功能 9）：一次通话录音的转写 + AI 复盘结果。
 * 录音由外呼供应商回调或手动上传回流，异步转写后复用会话军师分析链路出复盘结论。
 */
@Data
@Table(name = "ai_call_record")
public class AiCallRecord extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "外呼供应商名(回调带入；手动上传为空)")
    private String supplier;

    @Schema(description = "主叫号码")
    private String caller;

    @Schema(description = "被叫号码")
    private String callee;

    @Schema(description = "客户号码(用于匹配客户)")
    private String customerPhone;

    @Schema(description = "关联客户ID(可空)")
    private String customerId;

    @Schema(description = "通话时间(毫秒)")
    private Long callTime;

    @Schema(description = "通话时长(秒)")
    private Integer duration;

    @Schema(description = "录音公网URL")
    private String recordUrl;

    @Schema(description = "手动上传的录音附件ID(可空)")
    private String recordAttachmentId;

    @Schema(description = "语音转写文本")
    private String transcript;

    @Schema(description = "复盘结果JSON(结构=会话军师结构化字段)")
    private String reviewJson;

    @Schema(description = "状态: PENDING_TRANSCRIBE/TRANSCRIBING/ANALYZING/DONE/FAILED")
    private String status;

    @Schema(description = "ASR任务ID(重试用)")
    private String asrTaskId;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "一键转跟进生成的跟进记录ID(可空，用于禁用重复转跟进)")
    private String followRecordId;
}
