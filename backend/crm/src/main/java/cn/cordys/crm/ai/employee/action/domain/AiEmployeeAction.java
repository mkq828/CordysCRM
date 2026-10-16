package cn.cordys.crm.ai.employee.action.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 员工待确认动作（功能 6 二期「自动跟进闭环」的持久化落点）。
 * <p>
 * 定时扫描「久未联系」客户后生成一条 {@code PENDING} 动作，客户负责人确认后执行（本期只支持
 * {@link #ACTION_FOLLOW_UP_CREATE} 写跟进记录）或驳回。动作类型在此扩展，后续可支持打标签、线索流转等。
 * </p>
 */
@Data
@Table(name = "ai_employee_action")
public class AiEmployeeAction extends BaseModel {

    /** 动作类型：创建跟进记录（本期唯一） */
    public static final String ACTION_FOLLOW_UP_CREATE = "FOLLOW_UP_CREATE";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_EXECUTED = "EXECUTED";
    public static final String STATUS_FAILED = "FAILED";

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "岗位 Agent ID（定时扫描无岗位，可空）")
    private String agentId;

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "客户名称（冗余展示）")
    private String customerName;

    @Schema(description = "客户负责人（确认人）")
    private String ownerId;

    @Schema(description = "动作载荷（JSON：content/followMethod/suggestion）")
    private String payload;

    @Schema(description = "状态：PENDING/APPROVED/REJECTED/EXECUTED/FAILED")
    private String status;

    @Schema(description = "执行结果（可空）")
    private String result;

    @Schema(description = "确认人")
    private String confirmUser;

    @Schema(description = "确认时间（毫秒）")
    private Long confirmTime;
}
