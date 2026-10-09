package cn.cordys.crm.ai.video.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 短视频生成-风格页「模型」区块返回。图片模型来自「模型设置」里已启用的豆包 Seedream；视频模型当前为空（静图成片）。
 */
@Data
public class VideoModelsResponse {

    @Schema(description = "图片模型（文生图）")
    private List<VideoModelOption> imageModels;

    @Schema(description = "视频模型（成片方式为 AI 视频时才使用，当前静图成片为空）")
    private List<VideoModelOption> videoModels;
}
