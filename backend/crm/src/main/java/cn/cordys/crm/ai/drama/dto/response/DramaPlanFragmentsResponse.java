package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分镜规划响应（异步：queued 为 true 时前端轮询分集 status / fragments）。
 */
@Data
public class DramaPlanFragmentsResponse {

    @Schema(description = "是否真正入队异步规划（已在进行中则为 false）")
    private boolean queued;

    @Schema(description = "分集（含规划状态）")
    private DramaEpisodeResponse episode;
}
