package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 生成分集画面/视频请求（指定分镜，空则整集）。
 */
@Data
public class DramaGenerateRequest {

    @Schema(description = "指定分镜ID列表（空则整集所有分镜）")
    private List<String> fragmentIds;

    @Schema(description = "图片/视频模型ID（空则取推荐）")
    private String modelId;

    @Schema(description = "成片方式: image(静图+口播)/video(图生视频)，默认 image")
    private String mode;
}
