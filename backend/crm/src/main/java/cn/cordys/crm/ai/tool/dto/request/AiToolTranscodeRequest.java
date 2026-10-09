package cn.cordys.crm.ai.tool.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 视频转格式工具请求：源视频为已上传的临时附件，转成目标格式。
 */
@Data
public class AiToolTranscodeRequest {

    @Schema(description = "源视频附件ID（上传临时附件返回的 ID）")
    @NotBlank(message = "请先上传源视频")
    private String sourceAttachmentId;

    @Schema(description = "源视频原始文件名（用于转格式后沿用文件名，仅替换扩展名）")
    private String sourceName;

    @Schema(description = "目标格式: mp4/mov/gif/webm，默认 mp4")
    private String format;
}
