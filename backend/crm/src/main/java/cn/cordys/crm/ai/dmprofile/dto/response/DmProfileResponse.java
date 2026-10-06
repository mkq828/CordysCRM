package cn.cordys.crm.ai.dmprofile.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 私信画像结果。画像字段在 {@link #profileJson} 里以结构化 JSON 返回，由前端解析渲染。
 */
@Data
public class DmProfileResponse {

    @Schema(description = "画像主键")
    private String id;

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

    @Schema(description = "主页简介(签名/认证文本)")
    private String bio;

    @Schema(description = "是否群聊")
    private Boolean isGroup;

    @Schema(description = "手机号(正则提取)")
    private String phone;

    @Schema(description = "微信号(正则提取)")
    private String wechat;

    @Schema(description = "状态: RAW/ANALYZED/IMPORTED/FAILED")
    private String status;

    @Schema(description = "画像JSON(性别/年龄/城市/品类/喜好/话术汇总/置信度)")
    private String profileJson;

    @Schema(description = "线索ID(已导入时)")
    private String clueId;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;
}
