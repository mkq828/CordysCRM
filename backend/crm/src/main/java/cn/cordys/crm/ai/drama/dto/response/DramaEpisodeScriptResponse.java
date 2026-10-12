package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 生成分集剧本响应：queued 表示是否入队异步；totalGenerated/totalTarget 供前端展示进度。
 */
@Data
public class DramaEpisodeScriptResponse {

    @Schema(description = "是否已入队异步生成")
    private boolean queued;

    @Schema(description = "已完成（正文达标）集数")
    private int totalGenerated;

    @Schema(description = "目标总集数")
    private int totalTarget;

    @Schema(description = "是否已全部完成")
    private boolean done;

    @Schema(description = "最新剧本")
    private DramaScriptResponse script;
}
