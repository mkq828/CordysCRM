package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 生成剧情摘要响应：queued 表示本次是否真的入队异步生成（已在跑则 false）。
 */
@Data
public class DramaScriptSummaryResponse {

    @Schema(description = "是否已入队异步生成")
    private boolean queued;

    @Schema(description = "最新剧本")
    private DramaScriptResponse script;
}
