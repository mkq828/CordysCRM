package cn.cordys.crm.ai.video.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 创作中心-视频模板：系统预置/可扩展的视频风格模板。
 * config_json 驱动合成（配色/字幕/片头片尾/转场/BGM）与生成提示词（脚本语气/画面风格）。
 */
@Data
@Table(name = "ai_video_template")
public class AiVideoTemplate extends BaseModel {

    @Schema(description = "模板标识(唯一，任务回填)")
    private String templateKey;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "前端卡片主色(如 #7c3aed)")
    private String color;

    @Schema(description = "前端卡片浅色(如 #ede9fe)")
    private String softColor;

    @Schema(description = "合成/生成配置 JSON")
    private String configJson;

    @Schema(description = "排序(越小越靠前)")
    private Integer sort;

    @Schema(description = "是否启用(1是/0否)")
    private Integer enabled;
}
