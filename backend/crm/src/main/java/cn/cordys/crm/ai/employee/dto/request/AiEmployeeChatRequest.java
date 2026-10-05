package cn.cordys.crm.ai.employee.dto.request;

import lombok.Data;

/**
 * AI 员工岗位 Agent 对话请求（流式）。
 */
@Data
public class AiEmployeeChatRequest {

    /** 用户消息 */
    private String message;

    /** 会话 ID（可空；为空时服务端新建会话） */
    private String conversationId;

    /** 此前同事发言（可空；虚拟会议室等场景按顺序注入，供当前岗位参考以避免重复） */
    private String context;

    /** 会议阶段（可空；speak=发言轮，discuss=讨论轮，空=普通 1 对 1 对话） */
    private String stage;
}
