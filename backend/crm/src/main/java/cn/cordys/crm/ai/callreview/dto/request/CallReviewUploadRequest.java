package cn.cordys.crm.ai.callreview.dto.request;

import lombok.Data;

/**
 * 手动上传/粘贴录音发起复盘：录音公网 URL 或本地录音附件二选一，其余为可选补充信息。
 */
@Data
public class CallReviewUploadRequest {

    /** 录音公网 URL（粘贴） */
    private String recordUrl;

    /** 本地录音附件 ID（临时附件，可空） */
    private String recordAttachmentId;

    /** 关联客户 ID（可空；复盘结果沉淀到该客户画像） */
    private String customerId;

    /** 主叫号码（可空） */
    private String caller;

    /** 被叫号码（可空） */
    private String callee;

    /** 客户号码（可空） */
    private String customerPhone;

    /** 通话时间(毫秒，可空) */
    private Long callTime;

    /** 通话时长(秒，可空) */
    private Integer duration;
}
