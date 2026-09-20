package cn.cordys.crm.suggestion.enums;

import lombok.Getter;

/**
 * 需求反馈状态流转：待处理 → 已采纳 → 开发中 → 已上线；或 已拒绝。
 */
@Getter
public enum SuggestionStatus {
    PENDING,
    ADOPTED,
    DEVELOPING,
    RELEASED,
    REJECTED
}
