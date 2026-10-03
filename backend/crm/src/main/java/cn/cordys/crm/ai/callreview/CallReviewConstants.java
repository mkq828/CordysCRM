package cn.cordys.crm.ai.callreview;

import java.util.List;

/**
 * 智能通话复盘常量：状态机、CRM 标准字段（供回调字段映射使用）。
 */
public final class CallReviewConstants {

    // ==================== 状态机 ====================
    public static final String STATUS_PENDING_TRANSCRIBE = "PENDING_TRANSCRIBE";
    public static final String STATUS_TRANSCRIBING = "TRANSCRIBING";
    public static final String STATUS_ANALYZING = "ANALYZING";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_FAILED = "FAILED";

    // ==================== CRM 标准字段（字段映射的 key） ====================
    /** 主叫号码 */
    public static final String FIELD_CALLER = "caller";
    /** 被叫号码 */
    public static final String FIELD_CALLEE = "callee";
    /** 客户号码 */
    public static final String FIELD_CUSTOMER_PHONE = "customerPhone";
    /** 通话时间(毫秒时间戳) */
    public static final String FIELD_CALL_TIME = "callTime";
    /** 通话时长(秒) */
    public static final String FIELD_DURATION = "duration";
    /** 录音文件地址(公网URL) */
    public static final String FIELD_RECORD_URL = "recordUrl";

    /** 回调 body 里允许映射的 CRM 标准字段全集 */
    public static final List<String> STANDARD_FIELDS = List.of(
            FIELD_CALLER, FIELD_CALLEE, FIELD_CUSTOMER_PHONE,
            FIELD_CALL_TIME, FIELD_DURATION, FIELD_RECORD_URL);

    private CallReviewConstants() {
    }
}
