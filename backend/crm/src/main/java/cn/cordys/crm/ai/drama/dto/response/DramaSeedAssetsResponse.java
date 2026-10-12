package cn.cordys.crm.ai.drama.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 资产抽取响应。
 */
@Data
public class DramaSeedAssetsResponse {

    @Schema(description = "抽取状态: generating/done/failed")
    private String status;

    @Schema(description = "是否已入队异步任务")
    private boolean queued;

    @Schema(description = "本次新建资产数（完成后回填到项目 params.assets_seed_created）")
    private Integer createdCount;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "当前资产列表")
    private List<DramaAssetResponse> assets;
}
