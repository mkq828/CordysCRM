package cn.cordys.crm.ai.video.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 视频模板库选项（前端模板选择卡片用，不暴露完整 config_json）。
 */
@Data
public class VideoTemplateOption {

    @Schema(description = "模板标识(提交生成时回传)")
    private String templateKey;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "前端卡片主色")
    private String color;

    @Schema(description = "前端卡片浅色")
    private String softColor;

    @Schema(description = "片头/片尾底色(封面预览用, #RRGGBB)")
    private String background;

    @Schema(description = "片尾引导文案(封面预览用)")
    private String closingText;
}
