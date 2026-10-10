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

    @Schema(description = "火山语音 AppID（列表脱敏，详情完整）")
    private String appId;

    @Schema(description = "火山语音 AccessToken（列表脱敏，详情完整）")
    private String accessToken;

    @Schema(description = "火山语音 SecretKey（列表脱敏，详情完整）")
    private String secretKey;

    @Schema(description = "启用状态")
    private Boolean enable;

    @Schema(description = "用户每日调用限制")
    private Long userDailyLimit;

    @Schema(description = "全局每日调用限制")
    private Long globalDailyLimit;

    @Schema(description = "模型参数（JSON）")
    private String modelParams;

    @Schema(description = "声音克隆模型名（阿里云声音复刻 target_model，留空回退默认）")
    private String voiceCloneModel;

    @Schema(description = "模型描述（短视频风格页展示）")
    private String description;

    @Schema(description = "是否推荐模型")
    private Boolean recommended;

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
