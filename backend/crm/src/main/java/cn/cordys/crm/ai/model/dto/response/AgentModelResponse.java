package cn.cordys.crm.ai.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 模型列表/详情响应。
 */
@Data
public class AgentModelResponse {

    @Schema(description = "模型ID")
    private String id;

    @Schema(description = "模型展示名称")
    private String displayName;

    @Schema(description = "模型名称（API identifier）")
    private String modelName;

    @Schema(description = "模型供应商")
    private String provider;

    @Schema(description = "API请求地址")
    private String apiUrl;

    @Schema(description = "API Key（列表脱敏，详情完整）")
    private String apiKey;

    @Schema(description = "启用状态")
    private Boolean enable;

    @Schema(description = "用户每日调用限制")
    private Long userDailyLimit;

    @Schema(description = "全局每日调用限制")
    private Long globalDailyLimit;

    @Schema(description = "模型参数（JSON）")
    private String modelParams;

    @Schema(description = "今日调用量（tokens）")
    private Long dailyTotal;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人")
    private String updateUser;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;
}
