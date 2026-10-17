package cn.cordys.crm.ai.employee.action.dto;

import lombok.Data;

/**
 * 「感兴趣且近期待跟进」的线索行：线索自动流转扫描的候选，供 LLM 判断下一步阶段时的上下文。
 */
@Data
public class ClueFlowRow {

    private String clueId;

    private String clueName;

    /** 线索负责人 userId（确认人） */
    private String owner;

    /** 当前阶段（扫描条件固定为 INTERESTED） */
    private String stage;

    /** 最新跟进时间（毫秒） */
    private Long followTime;

    private String contact;

    private String phone;
}
