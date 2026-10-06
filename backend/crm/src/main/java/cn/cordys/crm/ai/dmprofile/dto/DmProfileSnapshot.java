package cn.cordys.crm.ai.dmprofile.dto;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.dmprofile.domain.AiDmProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 私信画像快照：线索转客户时，把 {@link AiDmProfile} 的画像字段拍平成一个单层 JSON，
 * 落库到 customer.dm_profile_json，供客户详情「私信画像」tab 结构化展示。
 * 前 9 个字段来自 profileJson，其余来自 ai_dm_profile 顶层列（含昵称/手机/微信/类型，
 * 与采集列表页展示字段一一对应，保证「采集 → 线索 → 客户」信息对称）。
 */
@Data
public class DmProfileSnapshot {

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "年龄段")
    private String age;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "账号品类")
    private String accountCategory;

    @Schema(description = "咨询品类")
    private String category;

    @Schema(description = "喜好")
    private String preference;

    @Schema(description = "话术汇总")
    private String summary;

    @Schema(description = "置信度")
    private String confidence;

    @Schema(description = "手机号(正则提取)")
    private String phone;

    @Schema(description = "微信号(正则提取)")
    private String wechat;

    @Schema(description = "主页简介")
    private String bio;

    @Schema(description = "抖音号")
    private String uniqueId;

    @Schema(description = "粉丝数")
    private String followerCount;

    @Schema(description = "关注数")
    private String followingCount;

    @Schema(description = "获赞数")
    private String likedCount;

    @Schema(description = "是否群聊")
    private Boolean isGroup;

    /** 从 ai_dm_profile 构建画像快照，拍平成单层 JSON 字段 */
    public static DmProfileSnapshot from(AiDmProfile profile) {
        DmProfileSnapshot snapshot = new DmProfileSnapshot();
        if (StringUtils.isNotBlank(profile.getProfileJson())) {
            try {
                snapshot = JSON.parseObject(profile.getProfileJson(), DmProfileSnapshot.class);
            } catch (Exception e) {
                snapshot = new DmProfileSnapshot();
            }
        }
        if (snapshot == null) {
            snapshot = new DmProfileSnapshot();
        }
        if (StringUtils.isBlank(snapshot.getGender())) {
            snapshot.setGender(profile.getGender());
        }
        snapshot.setNickname(profile.getNickname());
        snapshot.setPhone(profile.getPhone());
        snapshot.setWechat(profile.getWechat());
        snapshot.setIsGroup(profile.getIsGroup());
        snapshot.setBio(profile.getBio());
        snapshot.setUniqueId(profile.getUniqueId());
        snapshot.setFollowerCount(profile.getFollowerCount());
        snapshot.setFollowingCount(profile.getFollowingCount());
        snapshot.setLikedCount(profile.getLikedCount());
        return snapshot;
    }
}
