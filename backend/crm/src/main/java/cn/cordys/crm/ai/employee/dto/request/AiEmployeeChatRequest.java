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
}
