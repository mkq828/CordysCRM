package cn.cordys.crm.ai.content.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 单条获客内容：一份「可直接发布的物料包」。
 * 覆盖 选题 → 标题 → 正文 → 配图文案 → 封面文案 → 话题标签 → 发布时间 → 口播脚本，
 * 其中「选题 + 口播脚本」是二期功能 7（短视频成片）的输入。
 */
@Data
public class AiContentItem {

    @Schema(description = "选题（一句话角度）")
    private String topic;

    @Schema(description = "标题（发布标题，带钩子感）")
    private String title;

    @Schema(description = "正文文案（小红书/朋友圈正文，抖音可为口播字幕稿）")
    private String copy;

    @Schema(description = "配图文案（配图/贴纸上的短文案）")
    private String imageCopy;

    @Schema(description = "封面文案（抖音封面、小红书首图上的大字）")
    private String coverCopy;

    @Schema(description = "话题标签（2-4 个，带 # 号）")
    private List<String> hashtags;

    @Schema(description = "最佳发布时间（如「工作日 19:00-21:00」）")
    private String bestTime;

    @Schema(description = "口播脚本（视频口播稿，含开头钩子；朋友圈可留空）")
    private String script;
}
