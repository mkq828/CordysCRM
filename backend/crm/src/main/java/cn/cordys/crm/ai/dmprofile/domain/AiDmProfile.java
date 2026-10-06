package cn.cordys.crm.ai.dmprofile.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 私信获客画像（功能 8）：一条私信联系人的采集结果 + AI 画像。
 * 采集到的原始消息存 {@link #messagesJson}，画像走 CRM G1 模型层（挂 G2 额度）后存 {@link #profileJson}。
 */
@Data
@Table(name = "ai_dm_profile")
public class AiDmProfile extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "来源账号(ai_dm_account.id)")
    private String accountId;

    @Schema(description = "平台")
    private String platform;

    @Schema(description = "平台用户 sec_uid")
    private String secUid;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "账号ID(抖音号)")
    private String uniqueId;

    @Schema(description = "粉丝数(群聊固定'群聊')")
    private String followerCount;

    @Schema(description = "关注数(主页刮取)")
    private String followingCount;

    @Schema(description = "获赞数(主页刮取)")
    private String likedCount;

    @Schema(description = "性别(主页「抖音号」后的男/女文本，可能为空)")
    private String gender;

    @Schema(description = "主页简介(签名/认证文本，用于 AI 提炼账号品类)")
    private String bio;

    @Schema(description = "是否群聊")
    private Boolean isGroup;

    @Schema(description = "正则提取手机号")
    private String phone;

    @Schema(description = "正则提取微信号")
    private String wechat;

    @Schema(description = "私信原文(结构化 sender/content)")
    private String messagesJson;

    @Schema(description = "画像JSON(性别/年龄/城市/品类/喜好/话术汇总/置信度)")
    private String profileJson;

    @Schema(description = "状态: RAW/ANALYZED/IMPORTED/FAILED")
    private String status;

    @Schema(description = "导入线索池后的线索ID")
    private String clueId;
}
