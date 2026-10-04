package cn.cordys.crm.ai.employee.dto.request;

import lombok.Data;

import java.util.List;

/**
 * AI 员工虚拟会议室「生成会议纪要」请求：前端把董事长提问 + 各参会岗位的发言流水传给后端，
 * 后端用任务模型总结成结构化纪要并落库。
 */
@Data
public class AiMeetingMinutesGenerateRequest {

    /** 会议标题（可空；为空时由模型根据议题生成） */
    private String title;

    /** 会议时间文本（前端已按展示格式拼好，如「2026年10月5日 星期一 10:30 — 11:15（时长 45 分钟）」） */
    private String timeText;

    /** 主持人（董事长 = 登录账号） */
    private String host;

    /** 参会人名单（含董事长，按顺序） */
    private List<String> attendees;

    /** 发言流水（董事长提问 + 各岗位依次发言） */
    private List<Speech> transcript;

    @Data
    public static class Speech {

        /** 发言岗位名（董事长 / 销售总监 / …） */
        private String speaker;

        /** 发言内容 */
        private String content;
    }
}
