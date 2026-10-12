package cn.cordys.crm.ai.drama.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 漫剧分镜片段：一镜，含画面描述、口播台词、时长、成图/成片附件与引用资产。
 */
@Data
@Table(name = "ai_drama_fragment")
public class AiDramaFragment extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "所属分集ID")
    private String episodeId;

    @Schema(description = "镜序")
    private Integer sortOrder;

    @Schema(description = "画面描述(文生图 prompt)")
    private String content;

    @Schema(description = "口播台词(TTS + 字幕)")
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

    @Schema(description = "引用资产ID数组 JSON(角色/场景视觉锚点)")
    private String assetIdsJson;

    @Schema(description = "生成状态: generating/done/failed")
    private String status;

    @Schema(description = "附加信息 JSON")
    private String paramsJson;
}
