package cn.cordys.crm.ai.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 模型新增/更新请求。
 */
@Data
public class AgentModelSaveRequest {

    @Schema(description = "模型ID（更新时必填）")
    private String id;

    @Schema(description = "模型展示名称")
    private String displayName;

    @Schema(description = "模型名称（API identifier）")
    private String modelName;

    @Schema(description = "模型供应商")
    private String provider;

    @Schema(description = "API请求地址")
    private String apiUrl;

    @Schema(description = "API Key")
    private String apiKey;

    @Schema(description = "启用状态")
    private Boolean enable;

    @Schema(description = "用户每日调用限制")
    private Long userDailyLimit;

    @Schema(description = "全局每日调用限制")
    private Long globalDailyLimit;

    @Schema(description = "模型参数（JSON）")
    private String modelParams;
}
