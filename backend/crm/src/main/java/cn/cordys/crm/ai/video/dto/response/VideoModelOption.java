package cn.cordys.crm.ai.video.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 短视频生成-图片/视频模型选项（风格页「模型」区块用，跟随「模型设置」里 admin 已启用的模型）。
 */
@Data
public class VideoModelOption {

    @Schema(description = "模型 ID（agent_model.id）")
    private String id;

    @Schema(description = "展示名")
    private String label;

    @Schema(description = "说明（模型名/用途）")
    private String description;

    @Schema(description = "是否推荐（当前只有一个候选时标记推荐）")
    private boolean recommended;
}
