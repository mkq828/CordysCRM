package cn.cordys.crm.ai.dmprofile.dto;

import lombok.Data;

import java.util.List;

/**
 * Python 采集适配器返回的原始私信会话（未画像）。
 */
@Data
public class DmRawConversation {

    /** 会话标题（昵称） */
    private String name;

    /** 对方 sec_uid（群聊为空串） */
    private String secUid;

    /** 抖音号 */
    private String accountId;

    /** 粉丝数（群聊固定「群聊」） */
    private String followerCount;

    /** 关注数（主页刮取，可能为空） */
    private String followingCount;

    /** 获赞数（主页刮取，可能为空） */
    private String likedCount;

    /** 性别（主页「抖音号」后的男/女文本，可能为空） */
    private String gender;

    /** 主页简介（签名/认证文本，用于 AI 提炼账号品类，可能为空） */
    private String bio;

    /** 是否群聊 */
    private boolean isGroup;

    /** 私信消息（方向 + 内容） */
    private List<DmRawMessage> messages;
}
