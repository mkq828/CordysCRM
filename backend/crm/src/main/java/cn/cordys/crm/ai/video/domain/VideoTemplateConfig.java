package cn.cordys.crm.ai.video.domain;

/**
 * 视频模板配置：来自 ai_video_template.config_json，驱动 FFmpeg/ASS 合成与生成提示词。
 * 字段语义见种子迁移 V1.41.0_2。默认值与原「口播」硬编码完全一致，保证历史任务/无模板时行为不变。
 */
public record VideoTemplateConfig(
        String background,
        int titleFontSize,
        String titleColor,
        int subtitleFontSize,
        String subtitleColor,
        String subtitleOutline,
        String subtitleShadow,
        boolean subtitleCenter,
        String closingText,
        String transition,
        String bgmFile,
        double bgmVolume,
        String scriptStyle,
        String imageStyle) {

    /** 转场是否为片头/片尾淡入淡出 */
    public boolean isFade() {
        return "fade".equalsIgnoreCase(transition);
    }

    /** 是否配置了可用的 BGM（文件路径非空） */
    public boolean hasBgm() {
        return bgmFile != null && !bgmFile.isBlank();
    }

    /** 默认模板配置（与原口播管线硬编码一致） */
    public static VideoTemplateConfig defaults() {
        return new VideoTemplateConfig(
                "0x1E1B2E",
                96,
                "&H00FFFFFF",
                64,
                "&H00FFFFFF",
                "&H00000000",
                "&HA0000000",
                false,
                "关注主页 · 了解更多",
                "none",
                null,
                0.0,
                "亲切自然的口语化口播",
                "，竖屏9:16构图，高清写实电影质感");
    }
}
