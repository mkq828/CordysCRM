package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 漫剧分镜片段响应。
 */
@Data
public class DramaFragmentResponse {

    @Schema(description = "分镜ID")
    private String id;

    @Schema(description = "分集ID")
    private String episodeId;

    @Schema(description = "镜序")
    private Integer sortOrder;

    @Schema(description = "画面描述(文生图 prompt)")
    private String content;

    @Schema(description = "口播台词")
    private String narration;

    @Schema(description = "口播配音音频附件ID(wav/mp3)")
    private String narrationAttachmentId;

    @Schema(description = "本镜口播音色标识(空则用分集默认音色)")
    private String voice;

    @Schema(description = "时长(秒)")
    private Integer durationSec;

    @Schema(description = "画面/封面附件ID")
    private String coverAttachmentId;

    @Schema(description = "镜头视频附件ID")
    private String videoAttachmentId;

    @Schema(description = "引用资产ID数组 JSON")
    private String assetIdsJson;

    @Schema(description = "生成状态")
    private String status;
}
